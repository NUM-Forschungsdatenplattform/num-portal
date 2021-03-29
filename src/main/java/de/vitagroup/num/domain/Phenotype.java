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
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.ExpressionConverter;
import io.swagger.annotations.ApiModel;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@Entity
@ApiModel
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Phenotype {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String description;

  @Convert(converter = ExpressionConverter.class)
  private Expression query;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "owner_id")
  private UserDetails owner;

  public boolean hasEmptyOrDifferentOwner(String userId) {
    return ObjectUtils.isEmpty(owner) || !owner.getUserId().equals(userId);
  }
}
