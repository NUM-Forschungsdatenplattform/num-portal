package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.model.Project;
import de.vitagroup.num.domain.specification.ProjectSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomProjectRepository {

    Page<Project> findProjects(ProjectSpecification projectSpecification, Pageable pageable);
}
