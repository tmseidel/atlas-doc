package com.mdeg.docsportal.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PathSanitizerTest {

    @ParameterizedTest
    @CsvSource({
        "docs, docs",
        "wiki, wiki",
        "docs/, docs",
        "' ', root",
    })
    void shouldSanitizeSubdirectoryName(String input, String expected) {
        assertThat(PathSanitizer.sanitize(input)).isEqualTo(expected);
    }

    @Test
    void shouldHandleNullInput() {
        assertThat(PathSanitizer.sanitize(null)).isEqualTo("root");
    }

    @Test
    void shouldHandleBlankInput() {
        assertThat(PathSanitizer.sanitize("  ")).isEqualTo("root");
    }

    @Test
    void shouldHandleDotForRepoRoot() {
        assertThat(PathSanitizer.sanitize(".")).isEqualTo("root");
    }

    @Test
    void shouldRemoveTrailingSlashes() {
        assertThat(PathSanitizer.sanitize("docs///")).isEqualTo("docs");
    }

    @Test
    void shouldReplaceSpecialChars() {
        assertThat(PathSanitizer.sanitize("my docs & notes!")).isEqualTo("my_docs___notes_");
    }

    @Test
    void shouldReplaceHyphens() {
        assertThat(PathSanitizer.sanitize("my-docs")).isEqualTo("my_docs");
    }

    @Test
    void shouldHandleSubdirectoryWithSlash() {
        assertThat(PathSanitizer.sanitize("my-docs/v1")).isEqualTo("my_docs_v1");
    }
}
