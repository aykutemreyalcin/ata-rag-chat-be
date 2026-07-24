package com.ata.rag.ingestion.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heading-aware markdown chunking approximating 700-900 token windows with ~100 token overlap.
 * Token counts use a cheap char/4 heuristic (OpenAI-ish) so ingest works without tiktoken.
 */
public final class MarkdownChunker {
    private static final int MIN_TOKENS = 700;
    private static final int MAX_TOKENS = 900;
    private static final int OVERLAP_TOKENS = 100;
    private static final Pattern HEADING = Pattern.compile("^(#{1,4})\\s+(.*)$");

    private MarkdownChunker() {}

    public static List<TextChunk> chunkMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        List<Section> sections = splitIntoSections(markdown);
        List<Section> merged = mergeSmallSections(sections);
        List<TextChunk> chunks = new ArrayList<>();
        for (Section section : merged) {
            int tokens = countTokens(section.text());
            if (tokens <= MAX_TOKENS) {
                chunks.add(new TextChunk(section.breadcrumb(), section.text(), tokens));
            } else {
                chunks.addAll(splitOversized(section));
            }
        }
        return chunks;
    }

    static int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private static List<Section> splitIntoSections(String markdown) {
        List<HeadingLevel> stack = new ArrayList<>();
        List<Section> sections = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String line : markdown.split("\\R", -1)) {
            Matcher matcher = HEADING.matcher(line);
            if (matcher.matches()) {
                flush(sections, stack, current);
                int level = matcher.group(1).length();
                String title = matcher.group(2).trim();
                while (!stack.isEmpty() && stack.getLast().level() >= level) {
                    stack.removeLast();
                }
                stack.add(new HeadingLevel(level, title));
            }
            current.add(line);
        }
        flush(sections, stack, current);
        return sections;
    }

    private static void flush(List<Section> sections, List<HeadingLevel> stack, List<String> current) {
        String text = String.join("\n", current).trim();
        current.clear();
        if (text.isEmpty()) {
            return;
        }
        sections.add(new Section(breadcrumb(stack), breadcrumb(stack.subList(0, Math.max(0, stack.size() - 1))), text));
    }

    private static String breadcrumb(List<HeadingLevel> stack) {
        if (stack.isEmpty()) {
            return "Document";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) {
                builder.append(" > ");
            }
            builder.append(stack.get(i).title());
        }
        return builder.toString();
    }

    private static List<Section> mergeSmallSections(List<Section> sections) {
        List<Section> merged = new ArrayList<>();
        Section buffer = null;
        for (Section section : sections) {
            if (buffer == null) {
                buffer = section;
                continue;
            }
            boolean sameParent = buffer.parentBreadcrumb().equals(section.parentBreadcrumb());
            if (countTokens(buffer.text()) < MIN_TOKENS && sameParent) {
                buffer = new Section(
                        buffer.breadcrumb(),
                        buffer.parentBreadcrumb(),
                        buffer.text() + "\n\n" + section.text());
            } else {
                merged.add(buffer);
                buffer = section;
            }
        }
        if (buffer != null) {
            merged.add(buffer);
        }
        return merged;
    }

    private static List<TextChunk> splitOversized(Section section) {
        List<String> units = atomize(splitParagraphs(section.text()));
        return packUnits(units, section.breadcrumb());
    }

    private static List<String> splitParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        for (String part : text.split("\\n\\s*\\n")) {
            if (!part.isBlank()) {
                paragraphs.add(part.trim());
            }
        }
        return paragraphs;
    }

    private static List<String> atomize(List<String> paragraphs) {
        List<String> units = new ArrayList<>();
        for (String paragraph : paragraphs) {
            if (countTokens(paragraph) > MAX_TOKENS) {
                for (String sentence : paragraph.split("(?<=[.!?])\\s+")) {
                    if (!sentence.isBlank()) {
                        units.add(sentence.trim());
                    }
                }
            } else {
                units.add(paragraph);
            }
        }
        return units;
    }

    private static List<TextChunk> packUnits(List<String> units, String breadcrumb) {
        List<TextChunk> chunks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentTokens = 0;
        for (String unit : units) {
            int unitTokens = countTokens(unit);
            if (!current.isEmpty() && currentTokens + unitTokens > MAX_TOKENS) {
                chunks.add(makeChunk(breadcrumb, current, currentTokens));
                OverlapSeed seed = seedOverlap(current);
                current = new ArrayList<>(seed.units());
                currentTokens = seed.tokens();
            }
            current.add(unit);
            currentTokens += unitTokens;
        }
        if (!current.isEmpty()) {
            chunks.add(makeChunk(breadcrumb, current, currentTokens));
        }
        return chunks;
    }

    private static TextChunk makeChunk(String breadcrumb, List<String> units, int tokenCount) {
        return new TextChunk(breadcrumb, String.join("\n\n", units), tokenCount);
    }

    private static OverlapSeed seedOverlap(List<String> units) {
        List<String> seed = new ArrayList<>();
        int tokens = 0;
        for (int i = units.size() - 1; i >= 0; i--) {
            int unitTokens = countTokens(units.get(i));
            if (!seed.isEmpty() && tokens + unitTokens > OVERLAP_TOKENS) {
                break;
            }
            seed.add(0, units.get(i));
            tokens += unitTokens;
        }
        return new OverlapSeed(seed, tokens);
    }

    private record Section(String breadcrumb, String parentBreadcrumb, String text) {}

    private record HeadingLevel(int level, String title) {}

    private record OverlapSeed(List<String> units, int tokens) {}
}
