package org.remus.docsportal.service;

import org.remus.docsportal.dto.ProjectDto;
import org.remus.docsportal.model.entity.Project;
import org.remus.docsportal.repository.ProjectJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    public static final String DEFAULT_PROJECT_ID = "00000000-0000-0000-0000-000000000001";

    private final ProjectJpaRepository projectRepo;

    public List<ProjectDto> list() {
        getDefaultProject();
        return projectRepo.findAll().stream().map(this::toDto).toList();
    }

    public Project getRequired(String id) {
        return projectRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
    }

    public Project getDefaultProject() {
        return projectRepo.findById(DEFAULT_PROJECT_ID)
            .orElseGet(() -> projectRepo.save(Project.builder()
                .id(DEFAULT_PROJECT_ID)
                .name("Default Project")
                .description("Default project")
                .build()));
    }

    @Transactional
    public ProjectDto create(ProjectDto dto) {
        Project project = Project.builder()
            .id(UUID.randomUUID().toString())
            .name(dto.name().trim())
            .description(dto.description())
            .build();
        return toDto(projectRepo.save(project));
    }

    @Transactional
    public ProjectDto update(String id, ProjectDto dto) {
        Project project = getRequired(id);
        project.setName(dto.name().trim());
        project.setDescription(dto.description());
        return toDto(projectRepo.save(project));
    }

    public ProjectDto toDto(Project project) {
        return new ProjectDto(project.getId(), project.getName(), project.getDescription());
    }
}
