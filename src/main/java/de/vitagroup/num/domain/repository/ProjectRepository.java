package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, CustomProjectRepository {

  List<Project> findByStatusIn(List<ProjectStatus> statuses);

  List<Project> findByResearchers_UserIdAndStatusIn(String userId, List<ProjectStatus> statuses);

  List<Project> findByCoordinatorUserIdOrStatusIn(String userId, List<ProjectStatus> statuses);

  @Query(
      value =
          "SELECT * FROM project WHERE project.status IN (:status1,:status2,:status3) ORDER BY project.create_date DESC FETCH FIRST :count ROWS ONLY",
      nativeQuery = true)
  List<Project> findLatestProjects(
      int count, String status1, String status2, String status3);
}
