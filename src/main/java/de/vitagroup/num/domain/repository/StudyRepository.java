package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

  List<Study> findByCoordinatorUserId(String userId);

  List<Study> findByStatusIn(StudyStatus[] statuses);

  List<Study> findByResearchers_UserIdAndStatusIn(String userId, StudyStatus[] statuses);
}
