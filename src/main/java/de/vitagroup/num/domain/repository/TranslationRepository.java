package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    @Query("Select t.id from Translation t")
    List<Long> getAllTranslationsId();
}
