package org.highmed.numportal.service.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.highmed.numportal.domain.repository.ProjectRepository;
import org.springframework.stereotype.Component;

/**
 * Custom prometheus metric, to detect number of projects in different states, approved, ongoing, finished or archived.
 */
@Getter
@Component
public class ProjectsMetrics {
    private double totalNumberOfProjects;
    private double approvedProjects;
    private double ongoingProjects;
    private double finishedProjects;
    private double archivedProjects;

    public ProjectsMetrics(MeterRegistry registry, ProjectRepository projectRepository) {
        Gauge.builder("custom.metric.project.totalNumber.counter", this::getTotalNumberOfProjects)
                .description("Total number of projects")
                .register(registry);
        Gauge.builder("custom.metric.project.approved.counter", this::getApprovedProjects)
                .description("Approved projects")
                .register(registry);
        Gauge.builder("custom.metric.project.ongoing.counter", this::getOngoingProjects)
                .description("Ongoing projects")
                .register(registry);
        Gauge.builder("custom.metric.project.finished.counter", this::getFinishedProjects)
                .description("Finished projects")
                .register(registry);
        Gauge.builder("custom.metric.project.archived.counter", this::getArchivedProjects)
                .description("Archived projects")
                .register(registry);

        totalNumberOfProjects = projectRepository.findAll().size();

        //hier fehlen noch 4 stati der projekte

    }
}
