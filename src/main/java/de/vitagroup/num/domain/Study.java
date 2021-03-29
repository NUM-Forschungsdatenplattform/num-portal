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

package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import de.vitagroup.num.domain.repository.CategorySetConverter;
import de.vitagroup.num.domain.repository.MapConverter;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.StringSetConverter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"cohort", "transitions"})
public class Study {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String description;

  private String simpleDescription;

  private boolean usedOutsideEu;

  private String firstHypotheses;

  private String secondHypotheses;

  private String goal;

  @Enumerated(EnumType.STRING)
  private StudyStatus status;

  @Convert(converter = CategorySetConverter.class)
  private Set<StudyCategories> categories;

  @Convert(converter = StringSetConverter.class)
  private Set<String> keywords;

  private OffsetDateTime createDate;

  private OffsetDateTime modifiedDate;

  private LocalDate startDate;

  private LocalDate endDate;

  private boolean financed;

  @Convert(converter = MapConverter.class)
  private Map<String, String> templates;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "cohort_id", referencedColumnName = "id")
  private Cohort cohort;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "coordinator_id")
  private UserDetails coordinator;

  @ManyToMany
  @JoinTable(
      name = "study_users",
      joinColumns = @JoinColumn(name = "study_id"),
      inverseJoinColumns = @JoinColumn(name = "user_details_id"))
  private List<UserDetails> researchers;

  @ToString.Exclude
  @JsonManagedReference
  @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<StudyTransition> transitions = new HashSet<>();

  public boolean hasEmptyOrDifferentOwner(String userId) {
    return ObjectUtils.isEmpty(coordinator) || !coordinator.getUserId().equals(userId);
  }

  public boolean isStudyResearcher(String userId) {
    if (CollectionUtils.isEmpty(researchers)) {
      return false;
    }
    return researchers.stream().anyMatch(r -> userId.equals(r.getUserId()));
  }
}
