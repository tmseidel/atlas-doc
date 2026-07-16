package org.remus.docsportal.repository;

import org.remus.docsportal.model.entity.BuildRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildRecordJpaRepository extends JpaRepository<BuildRecord, String> {

    Optional<BuildRecord> findTopByProjectIdOrderByStartedAtDesc(String projectId);

    List<BuildRecord> findTop20ByProjectIdOrderByStartedAtDesc(String projectId);

    List<BuildRecord> findByProjectId(String projectId);
}
