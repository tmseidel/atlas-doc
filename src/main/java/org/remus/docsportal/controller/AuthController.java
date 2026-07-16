package org.remus.docsportal.controller;

import org.remus.docsportal.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final ProjectService projectService;

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/docs";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("projects", projectService.list());
        model.addAttribute("defaultProjectId", projectService.getDefaultProject().getId());
        return "login";
    }
}
