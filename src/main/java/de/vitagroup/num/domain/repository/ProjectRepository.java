package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Project;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, CustomProjectRepository {

  @Query(
      value =
          "SELECT * FROM project WHERE project.status IN (:status1,:status2,:status3) ORDER BY project.create_date DESC FETCH FIRST :count ROWS ONLY",
      nativeQuery = true)
  List<Project> findLatestProjects(
      int count, String status1, String status2, String status3);


  @Query(value = "SELECT new Project(pr.id, pr.name, pr.createDate, pr.coordinator) FROM Project pr " +
          "WHERE pr.status IN (:statuses) ORDER BY pr.createDate DESC ")
  List<Project> findByStatusInOrderByCreateDateDesc(List<ProjectStatus> statuses, Pageable pageable);
}
