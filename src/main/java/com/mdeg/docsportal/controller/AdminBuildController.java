package com.mdeg.docsportal.controller;

import com.mdeg.docsportal.dto.BuildRecordDto;
import com.mdeg.docsportal.model.entity.BuildStatus;
import com.mdeg.docsportal.repository.BuildRecordJpaRepository;
import com.mdeg.docsportal.service.BuildOrchestrator;
import com.mdeg.docsportal.service.ProjectContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/build")
@RequiredArgsConstructor
public class AdminBuildController {

    private final BuildOrchestrator buildOrchestrator;
    private final BuildRecordJpaRepository buildRecordRepo;
    private final ProjectContext projectContext;

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerBuild() {
        buildOrchestrator.triggerBuild("manual", "admin", projectContext.getProjectId());
        return ResponseEntity.accepted().body("Build triggered");
    }

    @GetMapping("/status")
    public ResponseEntity<BuildStatus> getStatus() {
        return ResponseEntity.ok(buildOrchestrator.getCurrentBuildStatus(projectContext.getProjectId()));
    }

    @GetMapping("/log")
    public ResponseEntity<String> getLog() {
        return buildRecordRepo.findTopByProjectIdOrderByStartedAtDesc(projectContext.getProjectId())
            .map(r -> ResponseEntity.ok(r.getLogOutput() != null ? r.getLogOutput() : ""))
            .orElse(ResponseEntity.ok(""));
    }

    @GetMapping("/history")
    public ResponseEntity<List<BuildRecordDto>> getHistory() {
        List<BuildRecordDto> history = buildRecordRepo
            .findTop20ByProjectIdOrderByStartedAtDesc(projectContext.getProjectId())
            .stream()
            .map(BuildRecordDto::from)
            .toList();
        return ResponseEntity.ok(history);
    }
}
