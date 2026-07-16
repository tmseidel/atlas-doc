package org.remus.docsportal.repository;

import org.remus.docsportal.model.entity.RepositoryConfig;
import org.remus.docsportal.model.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepositoryConfigJpaRepository extends JpaRepository<RepositoryConfig, String> {

    List<RepositoryConfig> findByProjectIdAndEnabledTrue(String projectId);

    List<RepositoryConfig> findByProjectId(String projectId);

    List<RepositoryConfig> findByName(String name);

}
