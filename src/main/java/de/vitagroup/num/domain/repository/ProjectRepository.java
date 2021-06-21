package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

  List<Project> findByCoordinatorUserId(String userId);

  List<Project> findByStatusIn(ProjectStatus[] statuses);

  List<Project> findByResearchers_UserIdAndStatusIn(String userId, ProjectStatus[] statuses);

  @Query(
      value =
          "SELECT * FROM project WHERE project.status IN (:status1,:status2,:status3) ORDER BY project.create_date DESC FETCH FIRST :count ROWS ONLY",
      nativeQuery = true)
  List<Project> findLatestProjects(
      int count, String status1, String status2, String status3);
}