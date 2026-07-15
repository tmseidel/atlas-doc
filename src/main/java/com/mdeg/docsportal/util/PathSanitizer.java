package com.mdeg.docsportal.util;

/**
 * Sanitizes subdirectory names for MkDocs docs/ paths.
 */
public final class PathSanitizer {

    private PathSanitizer() {}

    /**
     * Converts a subdirectory path to a safe MkDocs directory name.
     * "." -> "root"
     * "docs/" -> "docs"
     * "my-docs/v1" -> "my_docs_v1"
     */
    public static String sanitize(String subDir) {
        if (subDir == null || subDir.isBlank()) {
            return "root";
        }
        if (".".equals(subDir.trim())) {
            return "root";
        }
        return subDir
            .trim()
            .replaceAll("/+$", "")
            .replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
