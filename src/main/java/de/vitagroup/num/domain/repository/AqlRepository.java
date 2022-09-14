package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Aql;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AqlRepository extends JpaRepository<Aql, Long> {

  @Query("SELECT aql FROM Aql aql WHERE aql.owner.userId = :ownerId OR aql.publicAql = true")
  List<Aql> findAllOwnedOrPublic(@Param("ownerId") String ownerId);

  @Query("SELECT aql FROM Aql aql WHERE aql.owner.userId = :ownerId OR aql.publicAql = true")
  Page<Aql> findAllOwnedOrPublic(@Param("ownerId") String ownerId, Pageable pageable);

  @Query(
      "SELECT aql FROM Aql aql "
          + "WHERE (cast(:name as string) is null OR UPPER(aql.name) like %:name% ) "
          + "AND (aql.owner.userId = :ownerId OR aql.publicAql = true) ")
  List<Aql> findAllOwnedOrPublicByName(
      @Param("ownerId") String ownerId, @Param("name") String name);

  @Query(
      "SELECT aql FROM Aql aql "
          + "WHERE (cast(:name as string) is null OR UPPER(aql.name) like %:name% ) "
          + "AND aql.owner.userId = :ownerId")
  List<Aql> findAllOwnedByName(@Param("ownerId") String ownerId, @Param("name") String name);

  @Query(
      "SELECT aql FROM Aql aql "
          + "WHERE (cast(:name as string) is null OR UPPER(aql.name) like %:name% ) "
          + "AND ((aql.owner.organization.id = :organizationId AND aql.publicAql = true) OR aql.owner.userId = :ownerId) ")
  List<Aql> findAllOrganizationOwnedByName(
      @Param("organizationId") Long organizationId,
      @Param("ownerId") String ownerId,
      @Param("name") String name);

  List<Aql> findByCategoryId(Long id);

  boolean existsById(Long id);

  @Query(
          "SELECT distinct aql FROM Aql aql "
                  + "WHERE (cast(:name as string) is null OR UPPER(aql.name) like %:name% ) "
                  + "AND (aql.owner.userId = :ownerId OR aql.publicAql = true)" +
                  "  OR (aql.owner.id IN :uuids AND aql.publicAql = true) ")
  Page<Aql> findAllOwnedOrPublicByName(@Param("ownerId") String ownerId, @Param("name") String name, @Param("uuids") Set<String> otherOwnersIds, Pageable pageable);

  @Query(
          "SELECT distinct aql FROM Aql aql "
                  + "WHERE (cast(:name as string) is null OR UPPER(aql.name) like %:name% ) "
                  + "AND aql.owner.userId = :ownerId")
  Page<Aql> findAllOwnedByName(@Param("ownerId") String ownerId, @Param("name") String name, Pageable pageable);

  @Query(
          "SELECT distinct aql FROM Aql aql "
                  + "WHERE (cast(:name as string) is null OR UPPER(aql.name) like %:name% ) "
                  + "AND ((aql.owner.organization.id = :organizationId AND aql.publicAql = true) OR aql.owner.userId = :ownerId) " +
                  "OR (aql.publicAql = true AND aql.owner.organization.id = :organizationId AND aql.owner.id IN :uuids) ")
  Page<Aql> findAllOrganizationOwnedByName(@Param("organizationId") Long organizationId, @Param("ownerId") String ownerId,
          @Param("name") String name, @Param("uuids") Set<String> otherOwnersIds, Pageable pageable);

}
