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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URL;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel(description = "The card structure with localized content")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LocalizedPart{
    @ApiModelProperty(value = "The localized title of the card", example = "image-4", required = true)
    @NotBlank
    @Size(min=1, max = 65)
    private String title;

    @ApiModelProperty(value = "The localized text of the card", example = "This is the best card ever!")
    @Size(max = 150)
    private String text;

  }

  @ApiModelProperty(value = "The english content of the card", required = true)
  @NotNull
  private LocalizedPart en;

  @ApiModelProperty(value = "The german content of the card", required = true)
  @NotNull
  private LocalizedPart de;


  @ApiModelProperty(value = "The id of image of the card", example = "image-4", required = true)
  @NotBlank
  private String imageId;

  @ApiModelProperty(value = "The URL of the card", example = "https://www.vitagroup.ag/")
  private URL url;
}
