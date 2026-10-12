package com.techstore.service.seo;

import java.util.regex.Pattern;

/**
 * Puts a page's own head tags into the React index.html, in place of its homepage defaults.
 * SEO.jsx later updates the same tags in place, so nothing is duplicated after React starts.
 */
public final class SpaHead {

    private static final String INDEXABLE = "index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1";

    /** The index.html tags that describe one page; all of them are replaced. */
    private static final Pattern PAGE_TAGS = Pattern.compile(
            "<title>.*?</title>"
                    + "|<meta\\s+(?:name|property)=\"(?:description|keywords|robots|og:[^\"]*|twitter:[^\"]*)\"[^>]*>"
                    + "|<link\\s+rel=\"canonical\"[^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * @param canonical null for pages that must not be indexed (404)
     * @param ogType    "product" or "website"
     */
    public record Tags(String title, String ogTitle, String description, String canonical,
                       String image, String imageAlt, String ogType, boolean indexable) {}

    private SpaHead() {}

    public static String render(String indexHtml, Tags tags) {
        int headEnd = indexHtml.indexOf("</head>");
        if (headEnd < 0) {
            throw new IllegalArgumentException("index.html has no </head>");
        }
        String head = PAGE_TAGS.matcher(indexHtml.substring(0, headEnd)).replaceAll("");
        return head + html(tags) + indexHtml.substring(headEnd);
    }

    private static String html(Tags t) {
        StringBuilder sb = new StringBuilder();
        sb.append("<title>").append(escape(t.title())).append("</title>");
        meta(sb, "name", "description", t.description());
        meta(sb, "name", "robots", t.indexable() ? INDEXABLE : "noindex");
        if (t.canonical() != null) {
            sb.append("<link rel=\"canonical\" href=\"").append(escape(t.canonical())).append("\">");
        }
        meta(sb, "property", "og:type", t.ogType());
        meta(sb, "property", "og:site_name", "Care Tech");
        meta(sb, "property", "og:locale", "bg_BG");
        meta(sb, "property", "og:title", t.ogTitle());
        meta(sb, "property", "og:description", t.description());
        meta(sb, "property", "og:image", t.image());
        meta(sb, "property", "og:image:secure_url", t.image());
        meta(sb, "property", "og:image:alt", t.imageAlt());
        if (t.canonical() != null) {
            meta(sb, "property", "og:url", t.canonical());
        }
        meta(sb, "name", "twitter:card", "summary_large_image");
        meta(sb, "name", "twitter:title", t.ogTitle());
        meta(sb, "name", "twitter:description", t.description());
        meta(sb, "name", "twitter:image", t.image());
        meta(sb, "name", "twitter:image:alt", t.imageAlt());
        return sb.toString();
    }

    private static void meta(StringBuilder sb, String attribute, String name, String content) {
        if (content == null || content.isBlank()) return;
        sb.append("<meta ").append(attribute).append("=\"").append(name)
                .append("\" content=\"").append(escape(content)).append("\">");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
