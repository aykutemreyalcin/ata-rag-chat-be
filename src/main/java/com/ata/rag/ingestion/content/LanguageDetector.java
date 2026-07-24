package com.ata.rag.ingestion.content;

import java.util.Locale;
import java.util.regex.Pattern;

public final class LanguageDetector {
    private static final Pattern POLISH =
            Pattern.compile("[ąćęłńóśźż]", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private LanguageDetector() {}

    public static String detect(String text) {
        if (text == null || text.isBlank()) {
            return "und";
        }
        String sample = text.substring(0, Math.min(text.length(), 4000)).toLowerCase(Locale.ROOT);
        if (POLISH.matcher(sample).find()
                || sample.contains(" oraz ")
                || sample.contains(" studnia")
                || sample.contains("czesne")
                || sample.contains("rekrutac")) {
            return "pl";
        }
        return "en";
    }
}
