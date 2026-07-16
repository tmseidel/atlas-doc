package org.remus.docsportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping("/repos")
    public String reposPage(Model model) {
        model.addAttribute("currentPage", "repos");
        return "admin/repos";
    }

    @GetMapping("/projects")
    public String projectsPage(Model model) {
        model.addAttribute("currentPage", "projects");
        return "admin/projects";
    }

    @GetMapping("/mkdocs-config")
    public String mkdocsConfigPage(Model model) {
        model.addAttribute("currentPage", "mkdocs-config");
        return "admin/mkdocs-config";
    }

    @GetMapping("/build")
    public String buildPage(Model model) {
        model.addAttribute("currentPage", "build");
        return "admin/build";
    }
}
