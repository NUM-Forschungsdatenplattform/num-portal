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

package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Type;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CohortGroupDto {

  @ApiModelProperty(value = "The unique identifier", example = "1")
  private Long id;

  @ApiModelProperty(value = "Cohort group operation to be applied to the children", example = "AND")
  private Operator operator;

  @ApiModelProperty(
      value =
          "Cohort group parameter map representing the name of the aql parameter and the corresponding value")
  private Map<String, Object> parameters;

  @ApiModelProperty(required = true, value = "Type of the cohort group", example = "PHENOTYPE")
  @NotNull(message = "Type cannot be null")
  private Type type;

  @ApiModelProperty(
      value =
          "Children of the cohort group in case the type of the group is: GROUP; can be other groups or phenotypes")
  private List<CohortGroupDto> children;

  @ApiModelProperty(
      value = "Reference to phenotype in case the type of the group is: PHENOTYPE",
      example = "2")
  private Long phenotypeId;

  @JsonIgnore
  public boolean isPhenotype() {
    return Type.PHENOTYPE == type;
  }

  @JsonIgnore
  public boolean isGroup() {
    return Type.GROUP == type;
  }
}
