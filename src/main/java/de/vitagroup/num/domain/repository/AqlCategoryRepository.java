package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.AqlCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AqlCategoryRepository extends JpaRepository<AqlCategory, Long> {
}
