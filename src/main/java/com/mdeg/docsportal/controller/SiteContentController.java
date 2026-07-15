package com.mdeg.docsportal.controller;

import com.mdeg.docsportal.service.ProjectContext;
import com.mdeg.docsportal.service.ProjectPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves the MkDocs output, including its directory-style URLs.
 *
 * MkDocs links pages as {@code section/page/}; unlike a web server, Spring's
 * generic file resource handler does not resolve that directory to
 * {@code index.html}. Resolving it here keeps all generated relative links
 * inside the iframe working.
 */
@Controller
@RequestMapping("/site")
@RequiredArgsConstructor
public class SiteContentController {

    private final ProjectContext projectContext;
    private final ProjectPaths projectPaths;

    @GetMapping({"", "/", "/{*path}"})
    public ResponseEntity<Resource> serve(@PathVariable(required = false) String path) {
        try {
            Path root = projectPaths.site(projectContext.getProjectId()).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                return ResponseEntity.notFound().build();
            }

            String relativePath = path == null ? "" : path.replaceFirst("^/", "");
            Path candidate = root.resolve(relativePath).normalize();
            if (!candidate.startsWith(root)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (Files.isDirectory(candidate)) {
                candidate = candidate.resolve("index.html");
            }
            if (!Files.isRegularFile(candidate)) {
                return ResponseEntity.notFound().build();
            }

            // Do not allow a symlink included in a repository to escape site/.
            if (!candidate.toRealPath().startsWith(root.toRealPath())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            MediaType contentType = MediaTypeFactory.getMediaType(candidate.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(contentType)
                .body(new FileSystemResource(candidate));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
