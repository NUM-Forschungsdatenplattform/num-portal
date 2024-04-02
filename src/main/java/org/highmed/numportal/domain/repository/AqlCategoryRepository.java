package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.AqlCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AqlCategoryRepository extends JpaRepository<AqlCategory, Long> {

  @Query(
          value = "SELECT * FROM aql_category ",
          countQuery = "SELECT COUNT (*) FROM aql_category",
          nativeQuery = true)
  Page<AqlCategory> findAllCategories(Pageable pageable);

  @Query(
          value = "SELECT * FROM aql_category ORDER BY aql_category.name->>'de' ASC",
          nativeQuery = true)
  List<AqlCategory> findAllCategories();
}
