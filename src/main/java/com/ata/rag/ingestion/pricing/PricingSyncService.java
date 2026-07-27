package com.ata.rag.ingestion.pricing;

import com.ata.rag.config.RagProperties;
import com.ata.rag.ingestion.content.ContentHashes;
import com.ata.rag.ingestion.content.LanguageDetector;
import com.ata.rag.ingestion.pipeline.PageIngestService;
import com.ata.rag.ingestion.pipeline.ProcessedPage;
import com.ata.rag.model.CrawlRunEntity;
import com.ata.rag.repository.CrawlRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PricingSyncService {
    private static final Logger log = LoggerFactory.getLogger(PricingSyncService.class);

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final PageIngestService pageIngestService;
    private final CrawlRunRepository crawlRunRepository;
    private final HttpClient httpClient;

    public PricingSyncService(
            RagProperties properties,
            ObjectMapper objectMapper,
            PageIngestService pageIngestService,
            CrawlRunRepository crawlRunRepository) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.pageIngestService = pageIngestService;
        this.crawlRunRepository = crawlRunRepository;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public CrawlRunEntity sync() {
        CrawlRunEntity run = new CrawlRunEntity();
        run.setRunType("pricing");
        run.setStartedAt(Instant.now());
        run.setStatus("running");
        run = crawlRunRepository.save(run);

        int updated = 0;
        int failed = 0;
        try {
            if (properties.pricingApiUrl() == null || properties.pricingApiUrl().isBlank()) {
                throw new IllegalStateException("PRICING_API_URL is not configured");
            }
            JsonNode root = fetchPricingJson();
            List<ProcessedPage> documents = normalize(root);
            run.setPagesDiscovered(documents.size());
            for (ProcessedPage document : documents) {
                try {
                    pageIngestService.ingest(document);
                    updated++;
                } catch (Exception exception) {
                    failed++;
                    log.warn("Failed pricing document {}: {}", document.url(), exception.toString());
                }
            }
            run.setPagesUpdated(updated);
            run.setPagesFailed(failed);
            run.setStatus(failed == 0 ? "success" : "partial");
        } catch (Exception exception) {
            log.error("Pricing sync failed", exception);
            run.setStatus("failed");
            run.setErrorSummary(exception.getMessage());
        } finally {
            run.setFinishedAt(Instant.now());
            crawlRunRepository.save(run);
        }
        return run;
    }

    JsonNode fetchPricingJson() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.pricingApiUrl()))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", properties.userAgent())
                .header("Accept", "application/json,text/plain,*/*")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Pricing API HTTP " + response.statusCode() + " at "
                    + response.uri());
        }
        return objectMapper.readTree(response.body());
    }

    List<ProcessedPage> normalize(JsonNode root) {
        List<ProcessedPage> documents = new ArrayList<>();
        String citation = properties.pricingCitationUrl();

        JsonNode raw = root.path("RAW");
        if (raw.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> languages = raw.fields();
            while (languages.hasNext()) {
                Map.Entry<String, JsonNode> languageEntry = languages.next();
                String language = languageEntry.getKey();
                Iterator<Map.Entry<String, JsonNode>> cities = languageEntry.getValue().fields();
                while (cities.hasNext()) {
                    Map.Entry<String, JsonNode> cityEntry = cities.next();
                    String city = cityEntry.getKey();
                    JsonNode cityNode = cityEntry.getValue();
                    if (cityNode.isArray()) {
                        // English RAW.en.<city>[] shape with eu/ne semester/year fees.
                        for (JsonNode row : cityNode) {
                            documents.add(documentForEnglishRow(citation, language, city, row));
                        }
                        continue;
                    }
                    if (!cityNode.isObject()) {
                        continue;
                    }
                    Iterator<Map.Entry<String, JsonNode>> modes = cityNode.fields();
                    while (modes.hasNext()) {
                        Map.Entry<String, JsonNode> modeEntry = modes.next();
                        String mode = modeEntry.getKey();
                        if (!modeEntry.getValue().isArray()) {
                            continue;
                        }
                        for (JsonNode row : modeEntry.getValue()) {
                            documents.add(documentForPolishRow(citation, language, city, mode, row));
                        }
                    }
                }
            }
        }

        StringBuilder index = new StringBuilder("# Tuition calculator index\n\n");
        index.append("Source: ").append(citation).append("\n\n");
        index.append("Official tuition amounts are ingested from the akademiata.pl calculator JSON ")
                .append("(RAW Polish r10/r12 installments and English eu/ne annual/semester fees).\n");
        index.append("Example programmes: Informatyka / Computer Engineering, ")
                .append("Computer networks and cybersecurity in Wrocław.\n");
        String indexMd = index.toString();
        documents.add(new ProcessedPage(
                citation,
                "ATA Tuition Calculator",
                indexMd,
                "pl",
                "pricing",
                ContentHashes.sha256(indexMd),
                200));

        return documents;
    }

    private ProcessedPage documentForPolishRow(
            String citation, String language, String city, String mode, JsonNode row) {
        String modeLabel = modeLabel(mode);
        String programme = text(row, "k");
        String specialization = text(row, "s");
        String title = programmeTitle(programme, specialization);
        String programmePage = text(row, "ps");
        String ak = text(row, "ak");
        int degree = row.path("deg").asInt(0);

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Tuition — ").append(title).append('\n');
        markdown.append("City: ").append(cityLabel(city)).append(" (").append(city).append(")\n");
        markdown.append("Language of calculator: ").append(language).append('\n');
        markdown.append("Study mode: ").append(modeLabel).append('\n');
        markdown.append("Degree cycle: ").append(degree).append('\n');
        if (programme != null) {
            markdown.append("Programme / field of study: ").append(programme).append('\n');
        }
        if (specialization != null) {
            markdown.append("Specialization: ").append(specialization).append('\n');
        }
        appendAmount(markdown, "Monthly tuition (10 installments)", row.path("r10"), "PLN");
        appendAmount(markdown, "Monthly tuition (12 installments)", row.path("r12"), "PLN");
        appendAmount(markdown, "Recruitment fee", row.path("rekr"), "PLN");
        appendAmount(markdown, "Enrollment fee (wpisowe)", row.path("wps"), "PLN");
        if (programmePage != null) {
            markdown.append("Programme page: ").append(programmePage).append('\n');
        }
        if (ak != null) {
            markdown.append("Programme key: ").append(ak).append('\n');
        }

        String md = markdown.toString().trim();
        String url = programmePage != null && !programmePage.isBlank()
                ? programmePage
                : citation + "#raw/" + language + "/" + city + "/" + mode + "/"
                        + slug(ak != null ? ak : title);
        return new ProcessedPage(
                url,
                "Tuition — " + title + " — " + cityLabel(city) + " (" + modeLabel + ")",
                md,
                LanguageDetector.detect(md),
                "pricing",
                ContentHashes.sha256(md),
                200);
    }

    private ProcessedPage documentForEnglishRow(
            String citation, String language, String city, JsonNode row) {
        String programme = text(row, "k");
        String specialization = text(row, "s");
        String title = programmeTitle(programme, specialization);
        String programmePage = text(row, "ps");
        String ak = text(row, "ak");
        int degree = row.path("deg").asInt(0);

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Tuition — ").append(title).append('\n');
        markdown.append("City: ").append(cityLabel(city)).append(" (").append(city).append(")\n");
        markdown.append("Language of calculator: ").append(language).append('\n');
        markdown.append("Study language: English\n");
        markdown.append("Degree cycle: ").append(degree).append('\n');
        if (programme != null) {
            markdown.append("Programme / field of study: ").append(programme).append('\n');
        }
        if (specialization != null) {
            markdown.append("Specialization: ").append(specialization).append('\n');
        }
        appendEnglishGroup(markdown, "EU / CIS / Ukraine", row.path("eu"));
        appendEnglishGroup(markdown, "Other countries", row.path("ne"));
        appendAmount(markdown, "Recruitment fee", row.path("rekr"), "EUR");
        appendAmount(markdown, "Enrollment fee (wpisowe)", row.path("wps"), "EUR");
        if (programmePage != null) {
            markdown.append("Programme page: ").append(programmePage).append('\n');
        }
        if (ak != null) {
            markdown.append("Programme key: ").append(ak).append('\n');
        }

        String md = markdown.toString().trim();
        String url = programmePage != null && !programmePage.isBlank()
                ? programmePage
                : citation + "#raw/" + language + "/" + city + "/"
                        + slug(ak != null ? ak : title);
        return new ProcessedPage(
                url,
                "Tuition — " + title + " — " + cityLabel(city) + " (English)",
                md,
                "en",
                "pricing",
                ContentHashes.sha256(md),
                200);
    }

    private static void appendEnglishGroup(StringBuilder markdown, String label, JsonNode group) {
        if (group == null || !group.isObject()) {
            return;
        }
        appendAmount(markdown, label + " — annual tuition", group.path("r"), "EUR");
        appendAmount(markdown, label + " — semester tuition", group.path("s"), "EUR");
    }

    private static void appendAmount(StringBuilder markdown, String label, JsonNode value, String currency) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return;
        }
        String amount = value.asText();
        if (amount == null || amount.isBlank() || "null".equalsIgnoreCase(amount)) {
            return;
        }
        markdown.append(label).append(": ").append(amount).append(' ').append(currency).append('\n');
    }

    private static String programmeTitle(String programme, String specialization) {
        if (programme == null || programme.isBlank()) {
            return specialization == null || specialization.isBlank() ? "Programme" : specialization;
        }
        if (specialization == null || specialization.isBlank()) {
            return programme;
        }
        return programme + " — " + specialization;
    }

    private static String modeLabel(String mode) {
        return switch (mode) {
            case "s" -> "stacjonarne / full-time";
            case "n" -> "niestacjonarne / part-time";
            default -> mode;
        };
    }

    private static String cityLabel(String city) {
        return switch (city) {
            case "wwa" -> "Warszawa";
            case "wro" -> "Wrocław";
            default -> city;
        };
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isNull() || value.isMissingNode()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() || "null".equalsIgnoreCase(text) ? null : text;
    }
}
