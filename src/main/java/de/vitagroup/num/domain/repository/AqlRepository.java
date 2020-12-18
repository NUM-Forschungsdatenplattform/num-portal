package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Aql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AqlRepository extends JpaRepository<Aql, Long> {

  @Query(
      "SELECT aql FROM Aql aql "
          + "WHERE (cast(:name as string) is null or aql.name like %:name% ) "
          + "and (cast(:organizationId as string) is null or aql.organizationId = :organizationId) "
          + "and (cast(:ownerId as string) is null or aql.owner.userId = :ownerId) ")
  List<Aql> findAqlByNameAndOrganizationAndOwner(
      @Param("name") String name,
      @Param("organizationId") String organizationId,
      @Param("ownerId") String ownerId);
}
