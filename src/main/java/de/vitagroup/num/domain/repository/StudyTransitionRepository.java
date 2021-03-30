package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.StudyTransition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyTransitionRepository extends JpaRepository<StudyTransition, Long> {

  Optional<List<StudyTransition>> findAllByStudyIdAndFromStatusAndToStatus(
      Long studyId, StudyStatus fromStatus, StudyStatus toStatus);
}
