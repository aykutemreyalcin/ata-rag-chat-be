package com.ata.rag.ingestion.content;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HtmlContentExtractorTest {

    @Test
    void stripsNavAndKeepsMain() {
        String html =
                """
                <html><head><title>Admissions</title></head>
                <body>
                  <nav>Menu</nav>
                  <main><h1>Welcome</h1><p>Apply online for Computer Science.</p></main>
                  <footer>Cookies</footer>
                </body></html>
                """;
        HtmlContentExtractor.ExtractedHtml extracted = HtmlContentExtractor.extract(html);
        String markdown = HtmlToMarkdown.convert(extracted.mainHtml());
        assertTrue(extracted.title().contains("Admissions"));
        assertTrue(markdown.toLowerCase().contains("computer science"));
        assertFalse(markdown.toLowerCase().contains("menu"));
    }
}
