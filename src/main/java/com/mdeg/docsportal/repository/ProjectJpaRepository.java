package com.mdeg.docsportal.repository;

import com.mdeg.docsportal.model.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectJpaRepository extends JpaRepository<Project, String> {
}
