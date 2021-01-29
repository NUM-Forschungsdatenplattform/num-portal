package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

  List<Study> findByCoordinatorUserId(String userId);

  List<Study> findByStatus(StudyStatus status);

  List<Study> findByCoordinatorUserIdAndStatus(String userId, StudyStatus status);

  List<Study> findByResearchers_UserIdAndStatus(String userId, StudyStatus status);

  List<Study> findByResearchers_UserId(String userId);
}
