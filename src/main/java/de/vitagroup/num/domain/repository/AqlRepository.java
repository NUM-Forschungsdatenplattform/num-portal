package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Aql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AqlRepository extends JpaRepository<Aql, Long>, JpaSpecificationExecutor<Aql> {

  @Query("SELECT aql FROM Aql aql WHERE aql.owner.userId = :ownerId OR aql.publicAql = true")
  List<Aql> findAllOwnedOrPublic(@Param("ownerId") String ownerId);

  List<Aql> findByCategoryId(Long id);

  boolean existsById(Long id);

  @Query("SELECT COUNT(aq) FROM Aql aq WHERE aq.category.id =:categoryId")
  long countByCategoryId(@Param("categoryId") Long categoryId);
}
