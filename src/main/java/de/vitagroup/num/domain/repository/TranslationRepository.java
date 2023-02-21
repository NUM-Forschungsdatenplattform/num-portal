package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
}
