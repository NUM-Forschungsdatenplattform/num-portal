package org.highmed.domain.repository;

import org.highmed.domain.model.Project;
import org.highmed.domain.specification.ProjectSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomProjectRepository {

    Page<Project> findProjects(ProjectSpecification projectSpecification, Pageable pageable);
}
