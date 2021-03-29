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
import de.vitagroup.num.domain.repository.MapConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "children")
public class CohortGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Type type;
  private Operator operator;

  @Convert(converter = MapConverter.class)
  private Map<String, Object> parameters;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "parent_group_id")
  private CohortGroup parent;

  @JsonManagedReference
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
  private Set<CohortGroup> children = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "phenotype_id", referencedColumnName = "id")
  private Phenotype phenotype;
}
