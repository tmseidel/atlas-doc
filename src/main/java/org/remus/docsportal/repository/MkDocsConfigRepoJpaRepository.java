package org.remus.docsportal.repository;

import org.remus.docsportal.model.entity.MkDocsConfigRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MkDocsConfigRepoJpaRepository extends JpaRepository<MkDocsConfigRepo, String> {

    Optional<MkDocsConfigRepo> findByProjectId(String projectId);
}
