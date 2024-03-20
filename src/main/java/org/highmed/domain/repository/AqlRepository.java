package org.highmed.domain.repository;

import org.highmed.domain.model.Aql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AqlRepository extends JpaRepository<org.highmed.domain.model.Aql, Long>, JpaSpecificationExecutor<org.highmed.domain.model.Aql> {

  @Query("SELECT aql FROM Aql aql WHERE aql.owner.userId = :ownerId OR aql.publicAql = true")
  List<org.highmed.domain.model.Aql> findAllOwnedOrPublic(@Param("ownerId") String ownerId);

  List<org.highmed.domain.model.Aql> findByCategoryId(Long id);

  boolean existsById(Long id);

  @Query("SELECT COUNT(aq) FROM Aql aq WHERE aq.category.id =:categoryId")
  long countByCategoryId(@Param("categoryId") Long categoryId);
}
