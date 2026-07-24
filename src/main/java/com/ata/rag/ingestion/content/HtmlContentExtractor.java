package com.ata.rag.ingestion.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

public final class HtmlContentExtractor {
    private static final String[] NOISE_SELECTORS = {
        "nav",
        "header",
        "footer",
        "aside",
        "[role=navigation]",
        "[role=banner]",
        "[role=contentinfo]",
        "script",
        "style",
        "noscript",
        "iframe"
    };

    private static final String[] CONTENT_SELECTORS = {
        "main", "article", "[role=main]", "#content", "#main-content", ".content", ".entry-content"
    };

    private HtmlContentExtractor() {}

    public record ExtractedHtml(String title, String mainHtml) {}

    public static ExtractedHtml extract(String html) {
        Document document = Jsoup.parse(html);
        String title = document.title();
        for (String selector : NOISE_SELECTORS) {
            document.select(selector).remove();
        }
        document.select("[id*=cookie], [class*=cookie], [id*=consent], [class*=consent], [class*=banner], [class*=sidebar], [class*=breadcrumb]")
                .remove();

        Element content = null;
        for (String selector : CONTENT_SELECTORS) {
            content = document.selectFirst(selector);
            if (content != null) {
                break;
            }
        }
        if (content == null) {
            content = document.body() != null ? document.body() : document;
        }

        String cleaned = Jsoup.clean(content.html(), Safelist.relaxed()
                .addTags("h1", "h2", "h3", "h4", "table", "thead", "tbody", "tr", "th", "td"));
        return new ExtractedHtml(blankToNull(title), cleaned);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
