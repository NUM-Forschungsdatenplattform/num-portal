package org.highmed.numportal.service.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.highmed.numportal.domain.model.ProjectStatus;
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
    private double publishedProjects;
    private double closedProjects;
    private double archivedProjects;

    public ProjectsMetrics(MeterRegistry registry, ProjectRepository projectRepository) {
        Gauge.builder("custom.metric.project.totalNumber.counter", this::getTotalNumberOfProjects)
                .description("Total number of projects")
                .register(registry);
        Gauge.builder("custom.metric.project.approved.counter", this::getApprovedProjects)
                .description("Approved projects")
                .register(registry);
        Gauge.builder("custom.metric.project.published.counter", this::getPublishedProjects)
                .description("Published projects")
                .register(registry);
        Gauge.builder("custom.metric.project.closed.counter", this::getClosedProjects)
                .description("Closed projects")
                .register(registry);
        Gauge.builder("custom.metric.project.archived.counter", this::getArchivedProjects)
                .description("Archived projects")
                .register(registry);

        totalNumberOfProjects = projectRepository.count();
        approvedProjects = projectRepository.countByStatus(ProjectStatus.APPROVED);
        publishedProjects = projectRepository.countByStatus(ProjectStatus.PUBLISHED);
        closedProjects = projectRepository.countByStatus(ProjectStatus.CLOSED);
        archivedProjects = projectRepository.countByStatus(ProjectStatus.ARCHIVED);
    }

    public void increaseTotalNumber(){
        totalNumberOfProjects++;
    }

    public void decreaseTotalNumber(){
        totalNumberOfProjects--;
    }

    public void changeStatus(ProjectStatus before, ProjectStatus after){
        if(before.equals(after)){
            return;
        }
        switch (before){
            case APPROVED -> approvedProjects --;
            case PUBLISHED -> publishedProjects --;
            case CLOSED -> closedProjects --;
            case ARCHIVED -> archivedProjects --;
        }

        switch (after){
            case APPROVED -> approvedProjects ++;
            case PUBLISHED -> publishedProjects ++;
            case CLOSED -> closedProjects ++;
            case ARCHIVED -> archivedProjects ++;
        }
    }

}
