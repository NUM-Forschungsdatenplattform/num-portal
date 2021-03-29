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
import java.io.Serializable;
import java.time.OffsetDateTime;
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
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aql implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String use;

  private String purpose;

  private String query;

  private boolean publicAql;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "owner_id")
  private UserDetails owner;

  private OffsetDateTime createDate;

  private OffsetDateTime modifiedDate;

  public boolean hasEmptyOrDifferentOwner(String userId) {
    return ObjectUtils.isEmpty(owner) || !owner.getUserId().equals(userId);
  }

  public boolean isExecutable(String userId) {
    return !ObjectUtils.isEmpty(owner) && (owner.getUserId().equals(userId) || isPublicAql());
  }

  public boolean isViewable(String userId) {
    return !ObjectUtils.isEmpty(owner) && (owner.getUserId().equals(userId) || isPublicAql());
  }
}
