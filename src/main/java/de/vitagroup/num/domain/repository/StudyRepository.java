package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

  List<Study> findByCoordinatorUserId(String userId);
}
