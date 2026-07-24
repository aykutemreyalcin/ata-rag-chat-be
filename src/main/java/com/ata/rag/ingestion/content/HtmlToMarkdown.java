package com.ata.rag.ingestion.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public final class HtmlToMarkdown {
    private HtmlToMarkdown() {}

    public static String convert(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Element root = Jsoup.parseBodyFragment(html).body();
        StringBuilder out = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            private int listDepth;

            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode textNode) {
                    out.append(textNode.text());
                    return;
                }
                if (!(node instanceof Element element)) {
                    return;
                }
                switch (element.normalName()) {
                    case "h1" -> out.append("\n\n# ");
                    case "h2" -> out.append("\n\n## ");
                    case "h3" -> out.append("\n\n### ");
                    case "h4" -> out.append("\n\n#### ");
                    case "p", "div", "section", "article", "tr" -> out.append("\n\n");
                    case "br" -> out.append("\n");
                    case "li" -> {
                        out.append("\n");
                        out.append("  ".repeat(Math.max(0, listDepth - 1)));
                        out.append("- ");
                    }
                    case "ul", "ol" -> listDepth++;
                    case "a" -> out.append('[');
                    case "strong", "b" -> out.append("**");
                    case "em", "i" -> out.append('*');
                    default -> {
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (!(node instanceof Element element)) {
                    return;
                }
                switch (element.normalName()) {
                    case "a" -> {
                        String href = element.hasAttr("href") ? element.attr("href") : "";
                        out.append("](").append(href).append(')');
                    }
                    case "strong", "b" -> out.append("**");
                    case "em", "i" -> out.append('*');
                    case "ul", "ol" -> listDepth = Math.max(0, listDepth - 1);
                    case "h1", "h2", "h3", "h4", "p", "div", "section", "li", "tr" -> out.append('\n');
                    default -> {
                    }
                }
            }
        }, root);

        return out.toString().replace('\u0000', ' ').replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }
}
