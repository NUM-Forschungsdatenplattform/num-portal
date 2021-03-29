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

import de.vitagroup.num.domain.validation.ValidCohort;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CohortDto {

    @ApiModelProperty(
            value = "The unique identifier",
            example = "1")
    private Long id;

    @ApiModelProperty(
            required = true,
            value = "The name of the cohort")
    @NotBlank(message = "Cohort name should not be blank")
    @NotNull(message = "Cohort name is mandatory")
    private String name;

    @ApiModelProperty(
            value = "The description of the cohort")
    private String description;

    @ApiModelProperty(
            required = true,
            value = "Reference to the study")
    @NotNull(message = "Id of the study is mandatory")
    private Long studyId;

    @ApiModelProperty(
            required = true,
            value = "Cohort phenotypes groups")
    @NotNull(message = "Cohort group is mandatory")
    @ValidCohort(message = "Invalid cohort group")
    private CohortGroupDto cohortGroup;

}
