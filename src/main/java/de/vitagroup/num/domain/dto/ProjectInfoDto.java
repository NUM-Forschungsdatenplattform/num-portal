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

import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectInfoDto {

  @ApiModelProperty(value = "Project create date")
  private OffsetDateTime createDate;

  @ApiModelProperty(value = "Project title")
  private String title;

  @ApiModelProperty(value = "Organization name")
  private String organization;

  @ApiModelProperty(value = "Coordinator name")
  private String coordinator;
}
