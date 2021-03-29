/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Aql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AqlRepository extends JpaRepository<Aql, Long> {

  @Query("SELECT aql FROM Aql aql WHERE aql.owner.userId = :ownerId OR aql.publicAql = true")
  List<Aql> findAllOwnedOrPublic(@Param("ownerId") String ownerId);

  @Query(
      "SELECT aql FROM Aql aql "
          + "WHERE (cast(:name as string) is null OR aql.name like %:name% ) "
          + "AND (aql.owner.userId = :ownerId OR aql.publicAql = true) ")
  List<Aql> findAllOwnedOrPublicByName(
      @Param("ownerId") String ownerId, @Param("name") String name);

  @Query(
      "SELECT aql FROM Aql aql "
          + "WHERE (cast(:name as string) is null OR aql.name like %:name% ) "
          + "AND aql.owner.userId = :ownerId")
  List<Aql> findAllOwnedByName(@Param("ownerId") String ownerId, @Param("name") String name);

  @Query(
      "SELECT aql FROM Aql aql "
          + "WHERE (cast(:name as string) is null OR aql.name like %:name% ) "
          + "AND ((aql.owner.organization.id = :organizationId AND aql.publicAql = true) OR aql.owner.userId = :ownerId) ")
  List<Aql> findAllOrganizationOwnedByName(
      @Param("organizationId") Long organizationId,
      @Param("ownerId") String ownerId,
      @Param("name") String name);

}
