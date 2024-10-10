package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.Translation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

}
