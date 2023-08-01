package de.vitagroup.num.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.vitagroup.num.domain.Translation;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
}
