package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemDto {

  @ApiModelProperty(value = "The tile of the navigation item", example = "Link 4")
  @NotBlank
  private String title;

  @ApiModelProperty(value = "The URL of the navigation item", example = "https://www.google.de/")
  @NotBlank
  private String url;
}
