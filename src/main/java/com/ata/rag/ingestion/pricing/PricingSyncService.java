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
                    Iterator<Map.Entry<String, JsonNode>> modes = cityEntry.getValue().fields();
                    while (modes.hasNext()) {
                        Map.Entry<String, JsonNode> modeEntry = modes.next();
                        String mode = modeEntry.getKey();
                        String modeLabel = "s".equals(mode) ? "stacjonarne" : "n".equals(mode) ? "niestacjonarne" : mode;
                        if (!modeEntry.getValue().isArray()) {
                            continue;
                        }
                        StringBuilder markdown = new StringBuilder();
                        markdown.append("# Tuition fees\n\n");
                        markdown.append("Language: ").append(language).append('\n');
                        markdown.append("City: ").append(cityLabel(city)).append('\n');
                        markdown.append("Study mode: ").append(modeLabel).append("\n\n");
                        for (JsonNode row : modeEntry.getValue()) {
                            String programme = text(row, "k");
                            String specialization = text(row, "s");
                            int degree = row.path("deg").asInt(0);
                            markdown.append("## ").append(programme);
                            if (specialization != null && !specialization.isBlank()) {
                                markdown.append(" — ").append(specialization);
                            }
                            markdown.append('\n');
                            markdown.append("- Degree cycle: ").append(degree).append('\n');
                            markdown.append("- Monthly tuition (10 installments): ").append(row.path("r10").asText()).append(" PLN\n");
                            markdown.append("- Monthly tuition (12 installments): ").append(row.path("r12").asText()).append(" PLN\n");
                            markdown.append("- Recruitment fee: ").append(row.path("rekr").asText()).append(" PLN\n");
                            markdown.append("- Enrollment fee (wpisowe): ").append(row.path("wps").asText()).append(" PLN\n");
                            if (row.hasNonNull("ps")) {
                                markdown.append("- Programme page: ").append(row.path("ps").asText()).append('\n');
                            }
                            markdown.append('\n');
                        }
                        String md = markdown.toString().trim();
                        String url = citation + "#raw/" + language + "/" + city + "/" + mode;
                        documents.add(new ProcessedPage(
                                url,
                                "Tuition " + cityLabel(city) + " / " + modeLabel + " / " + language,
                                md,
                                LanguageDetector.detect(md),
                                "pricing",
                                ContentHashes.sha256(md),
                                200));
                    }
                }
            }
        }

        // Keep a compact index document for Computer Science style questions.
        StringBuilder index = new StringBuilder("# Tuition calculator index\n\n");
        index.append("Source: ").append(citation).append("\n\n");
        index.append("Official tuition amounts are ingested from the akademiata.pl calculator JSON ")
                .append("(RAW/UABY/PROMOS). Example: Informatyka in Warszawa has monthly tuition fields r10/r12.\n");
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

    private static String cityLabel(String city) {
        return switch (city) {
            case "wwa" -> "Warszawa";
            case "wro" -> "Wrocław";
            default -> city;
        };
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
