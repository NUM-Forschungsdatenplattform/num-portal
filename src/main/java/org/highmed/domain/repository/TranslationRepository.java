package org.highmed.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.highmed.domain.model.Translation;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
}
