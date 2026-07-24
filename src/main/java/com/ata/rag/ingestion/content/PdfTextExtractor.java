package com.ata.rag.ingestion.content;

import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class PdfTextExtractor {
    private PdfTextExtractor() {}

    public static String extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text.replace('\u0000', ' ').trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to extract PDF text", exception);
        }
    }

    public static String extract(InputStream inputStream) throws IOException {
        return extract(inputStream.readAllBytes());
    }
}
