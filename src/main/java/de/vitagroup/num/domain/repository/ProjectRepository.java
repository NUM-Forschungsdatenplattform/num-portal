package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

  List<Project> findByStatusIn(List<ProjectStatus> statuses);

  List<Project> findByResearchers_UserIdAndStatusIn(String userId, List<ProjectStatus> statuses);

  List<Project> findByCoordinatorUserIdOrStatusIn(String userId, ProjectStatus[] statuses);

  @Query(
      value =
          "SELECT * FROM project WHERE project.status IN (:status1,:status2,:status3) ORDER BY project.create_date DESC FETCH FIRST :count ROWS ONLY",
      nativeQuery = true)
  List<Project> findLatestProjects(
      int count, String status1, String status2, String status3);

  @Query("SELECT new Project(pr.id, pr.name, pr.status, pr.startDate, pr.endDate, pr.coordinator) " +
          "FROM Project pr " +
          "INNER JOIN pr.coordinator cr " +
          "WHERE cr.userId = :userId OR pr.status IN :statuses")
  List<Project> findByCoordinatorUserIdORStatusIn(@Param("userId") String userId, @Param("statuses") List<ProjectStatus> statuses);
}
