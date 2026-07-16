package org.remus.docsportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocsController {

    @GetMapping("/docs")
    public String docsPage(Model model) {
        model.addAttribute("currentPage", "docs");
        return "docs";
    }

    @GetMapping("/doc")
    public String docRedirect() {
        return "redirect:/docs";
    }
}
