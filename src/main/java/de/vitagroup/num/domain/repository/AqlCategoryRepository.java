package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.AqlCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AqlCategoryRepository extends JpaRepository<AqlCategory, Long> {

  @Query(
      value = "SELECT * FROM aql_category ORDER BY aql_category.name->>'de' ASC",
      nativeQuery = true)
  List<AqlCategory> findAllCategories();
}
