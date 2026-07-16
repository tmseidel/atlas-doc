package org.remus.docsportal.service;

import org.remus.docsportal.dto.DocPageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Scans the MkDocs site/ output directory to discover available documentation pages.
 * Parses index.html and page HTML files to extract titles and URLs for the page selector.
 *
 * Note: Uses Jsoup for HTML parsing. If Jsoup is not available on classpath,
 * falls back to simple file-based discovery.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocScannerService {

    private final ProjectContext projectContext;
    private final ProjectPaths projectPaths;

    /**
     * Scans site/ for HTML pages and returns a list of page titles + URLs.
     * Falls back to filename-based discovery if Jsoup is unavailable.
     */
    public List<DocPageDto> scanPages() {
        List<DocPageDto> pages = new ArrayList<>();
        Path siteDir = projectPaths.site(projectContext.getProjectId());
        if (!Files.exists(siteDir)) {
            return pages;
        }

        try {
            // Try to parse index.html for navigation links
            Path indexFile = siteDir.resolve("index.html");
            if (Files.exists(indexFile)) {
                String html = Files.readString(indexFile);
                Document doc = Jsoup.parse(html);

                // MkDocs Material stores nav links in <nav> or <a> elements
                doc.select("a[href]").forEach(link -> {
                    String href = link.attr("href");
                    String title = link.text().trim();
                    if (!href.isEmpty() && !title.isEmpty() && !href.startsWith("http") && !href.startsWith("#") && !href.startsWith("javascript") && href.endsWith(".html")) {
                        pages.add(new DocPageDto(title, href));
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Jsoup parsing failed, falling back to file discovery: {}", e.getMessage());
        }

        // If Jsoup found nothing or failed, fall back to file discovery
        if (pages.isEmpty()) {
            pages.addAll(scanByFiles(siteDir));
        }

        return pages.stream()
            .sorted(Comparator.comparing(DocPageDto::url))
            .toList();
    }

    private List<DocPageDto> scanByFiles(Path siteDir) {
        List<DocPageDto> pages = new ArrayList<>();
        try (var stream = Files.walk(siteDir)) {
            stream.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".html"))
                .filter(p -> !p.getFileName().toString().equals("index.html"))
                .filter(p -> !p.getFileName().toString().startsWith("404"))
                .forEach(p -> {
                    String relative = siteDir.relativize(p).toString().replace('\\', '/');
                    String title = p.getFileName().toString().replace(".html", "");
                    pages.add(new DocPageDto(title, relative));
                });
        } catch (IOException e) {
            log.warn("Failed to scan site/ directory: {}", e.getMessage());
        }
        return pages;
    }
}
