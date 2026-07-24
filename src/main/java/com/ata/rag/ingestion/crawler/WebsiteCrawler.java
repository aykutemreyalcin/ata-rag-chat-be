package com.ata.rag.ingestion.crawler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WebsiteCrawler {
    private static final Pattern DEFAULT_DENY = Pattern.compile(
            "/login|/logowanie|/wp-admin|/admin|/moodle|/search|[?&]s=|/feed|/tag/|/category/",
            Pattern.CASE_INSENSITIVE);

    private final String baseUrl;
    private final String host;
    private final String userAgent;
    private final Pattern denyPattern;
    private final HttpClient httpClient;
    private final Duration timeout;

    public WebsiteCrawler(String baseUrl, String userAgent, int timeoutSeconds) {
        this(baseUrl, userAgent, timeoutSeconds, DEFAULT_DENY, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build());
    }

    WebsiteCrawler(
            String baseUrl, String userAgent, int timeoutSeconds, Pattern denyPattern, HttpClient httpClient) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.host = URI.create(this.baseUrl).getHost();
        this.userAgent = userAgent;
        this.denyPattern = denyPattern;
        this.httpClient = httpClient;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public RawPage fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        String contentTypeHeader = response.headers().firstValue("content-type").orElse("");
        String finalUrl = response.uri().toString();
        String contentType = classifyContentType(finalUrl, contentTypeHeader);
        String lastModified = response.headers().firstValue("last-modified").orElse(null);
        return new RawPage(finalUrl, response.statusCode(), contentType, response.body(), lastModified);
    }

    public List<String> discoverLinks(RawPage page) {
        if (!"html".equals(page.contentType())) {
            return List.of();
        }
        Document document = Jsoup.parse(new String(page.body()), page.url());
        Set<String> links = new LinkedHashSet<>();
        for (Element anchor : document.select("a[href]")) {
            String href = anchor.attr("abs:href").split("#", 2)[0].trim();
            if (href.isEmpty()
                    || href.startsWith("mailto:")
                    || href.startsWith("tel:")
                    || href.startsWith("javascript:")) {
                continue;
            }
            if (isAllowed(href)) {
                links.add(href);
            }
        }
        return new ArrayList<>(links);
    }

    public boolean isAllowed(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return false;
        }
        if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(host)) {
            return false;
        }
        return !denyPattern.matcher(url).find();
    }

    public String baseUrl() {
        return baseUrl;
    }

    private static String classifyContentType(String url, String contentTypeHeader) {
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        String lowerHeader = contentTypeHeader.toLowerCase(Locale.ROOT);
        if (lowerHeader.contains("application/pdf") || lowerUrl.endsWith(".pdf")) {
            return "pdf";
        }
        if (lowerHeader.contains("text/html") || lowerHeader.isBlank()) {
            return "html";
        }
        return "other";
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
