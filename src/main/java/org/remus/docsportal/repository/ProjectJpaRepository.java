package org.remus.docsportal.repository;

import org.remus.docsportal.model.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectJpaRepository extends JpaRepository<Project, String> {
}
