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

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {

  List<Study> findByCoordinatorUserId(String userId);

  List<Study> findByStatusIn(StudyStatus[] statuses);

  List<Study> findByResearchers_UserIdAndStatusIn(String userId, StudyStatus[] statuses);

  @Query(value = "SELECT * FROM study ORDER BY study.create_date DESC FETCH FIRST :count ROWS ONLY", nativeQuery = true)
  List<Study> findLatestProjects(int count);
}