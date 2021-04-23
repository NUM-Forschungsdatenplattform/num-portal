package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

  List<Study> findByCoordinatorUserId(String userId);

  List<Study> findByStatusIn(StudyStatus[] statuses);

  List<Study> findByResearchers_UserIdAndStatusIn(String userId, StudyStatus[] statuses);

  @Query(
      value =
          "SELECT * FROM study WHERE study.status IN (:status1,:status2,:status3) ORDER BY study.create_date DESC FETCH FIRST :count ROWS ONLY",
      nativeQuery = true)
  List<Study> findLatestProjects(
      int count, String status1, String status2, String status3);
}
