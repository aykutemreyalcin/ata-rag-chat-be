package com.ata.rag.ingestion.pipeline;

import com.ata.rag.ingestion.content.ContentHashes;
import com.ata.rag.ingestion.content.HtmlContentExtractor;
import com.ata.rag.ingestion.content.HtmlToMarkdown;
import com.ata.rag.ingestion.content.LanguageDetector;
import com.ata.rag.ingestion.content.PdfTextExtractor;
import com.ata.rag.ingestion.crawler.RawPage;
import org.springframework.stereotype.Component;

@Component
public class PageProcessor {
    public ProcessedPage process(RawPage page) {
        if ("pdf".equals(page.contentType())) {
            String markdown = PdfTextExtractor.extract(page.body());
            return build(page, null, markdown, "pdf");
        }
        if (!"html".equals(page.contentType())) {
            throw new IllegalArgumentException("Unsupported content type: " + page.contentType());
        }
        if (page.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + page.statusCode() + " for " + page.url());
        }
        String html = new String(page.body());
        HtmlContentExtractor.ExtractedHtml extracted = HtmlContentExtractor.extract(html);
        String markdown = HtmlToMarkdown.convert(extracted.mainHtml());
        if (markdown.isBlank()) {
            throw new IllegalStateException("Empty content after cleaning for " + page.url());
        }
        return build(page, extracted.title(), markdown, "html");
    }

    private ProcessedPage build(RawPage page, String title, String markdown, String sourceType) {
        String cleanedMarkdown = markdown.replace("\u0000", "");
        String cleanedTitle = title == null ? null : title.replace("\u0000", "");
        return new ProcessedPage(
                page.url(),
                cleanedTitle,
                cleanedMarkdown,
                LanguageDetector.detect(cleanedMarkdown),
                sourceType,
                ContentHashes.sha256(cleanedMarkdown),
                page.statusCode());
    }
}
