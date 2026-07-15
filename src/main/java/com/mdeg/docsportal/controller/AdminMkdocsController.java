package com.mdeg.docsportal.controller;

import com.mdeg.docsportal.dto.MkDocsConfigRepoDto;
import com.mdeg.docsportal.service.MkDocsConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mkdocs-config")
@RequiredArgsConstructor
public class AdminMkdocsController {

    private final MkDocsConfigService mkDocsConfigService;

    @GetMapping
    public ResponseEntity<MkDocsConfigRepoDto> getConfig() {
        MkDocsConfigRepoDto config = mkDocsConfigService.findConfig();
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @PutMapping
    public ResponseEntity<MkDocsConfigRepoDto> saveConfig(@Valid @RequestBody MkDocsConfigRepoDto dto) {
        return ResponseEntity.ok(mkDocsConfigService.save(dto));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncConfig() {
        mkDocsConfigService.triggerSync();
        return ResponseEntity.ok(java.util.Map.of("message", "Sync triggered"));
    }
}
