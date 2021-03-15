package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.StudyTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyTransitionRepository extends JpaRepository<StudyTransition, String> {

}
