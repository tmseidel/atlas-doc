package org.remus.docsportal.controller;

import org.remus.docsportal.dto.ProjectDto;
import org.remus.docsportal.service.ProjectContext;
import org.remus.docsportal.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final ProjectService projectService;
    private final ProjectContext projectContext;

    @GetMapping
    public List<ProjectDto> list() {
        return projectService.list();
    }

    @PostMapping
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectDto dto) {
        return ResponseEntity.ok(projectService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> update(@PathVariable String id, @Valid @RequestBody ProjectDto dto) {
        return ResponseEntity.ok(projectService.update(id, dto));
    }

    @PostMapping("/context/{id}")
    public ResponseEntity<Void> selectContext(@PathVariable String id) {
        projectContext.selectProject(id);
        return ResponseEntity.noContent().build();
    }
}
