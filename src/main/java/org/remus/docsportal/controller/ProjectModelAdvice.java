package org.remus.docsportal.controller;

import org.remus.docsportal.service.ProjectContext;
import org.remus.docsportal.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class ProjectModelAdvice {

    private final ProjectContext projectContext;
    private final ProjectService projectService;

    @ModelAttribute("activeProject")
    public String activeProject() {
        return projectService.getRequired(projectContext.getProjectId()).getName();
    }
}
