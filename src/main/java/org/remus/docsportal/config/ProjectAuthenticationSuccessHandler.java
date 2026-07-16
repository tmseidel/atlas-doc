package org.remus.docsportal.config;

import org.remus.docsportal.service.ProjectContext;
import org.remus.docsportal.service.ProjectService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ProjectAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ProjectContext projectContext;
    private final ProjectService projectService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException {
        String projectId = request.getParameter("projectId");
        if (projectId == null || projectId.isBlank()) {
            projectId = projectService.getDefaultProject().getId();
        }
        try {
            projectContext.selectProject(projectId);
            response.sendRedirect(request.getContextPath() + "/docs");
        } catch (IllegalArgumentException e) {
            request.getSession().invalidate();
            response.sendRedirect(request.getContextPath() + "/login?projectError");
        }
    }
}
