package com.ata.rag.generation;

public record GenerationResult(
        String text,
        String model,
        Integer promptTokens,
        Integer outputTokens) {

    public GenerationResult(String text, String model) {
        this(text, model, null, null);
    }
}
