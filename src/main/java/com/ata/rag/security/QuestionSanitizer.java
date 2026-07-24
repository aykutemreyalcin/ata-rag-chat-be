package com.ata.rag.security;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class QuestionSanitizer {
    private static final int MAX_LENGTH = 2_000;
    private static final Pattern PROMPT_INJECTION = Pattern.compile(
            "(ignore|forget|disregard).{0,40}(previous|above|system|instructions)"
                    + "|(reveal|show|print).{0,30}(system prompt|hidden prompt|developer message)"
                    + "|jailbreak|do anything now",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public String sanitize(String question) {
        if (question == null) {
            throw new IllegalArgumentException("question is required");
        }
        String cleaned = question
                .replace('\u0000', ' ')
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (cleaned.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("question must be at most " + MAX_LENGTH + " characters");
        }
        if (PROMPT_INJECTION.matcher(cleaned.toLowerCase(Locale.ROOT)).find()) {
            throw new IllegalArgumentException("question contains unsupported prompt instructions");
        }
        return cleaned;
    }
}
