package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.specification.ProjectSpecification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomProjectRepository {

  Page<Project> findProjects(ProjectSpecification projectSpecification, Pageable pageable);
}
