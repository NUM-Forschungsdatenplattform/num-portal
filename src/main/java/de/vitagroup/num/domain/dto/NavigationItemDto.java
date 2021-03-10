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
@ApiModel
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemDto {

  @ApiModelProperty(value = "The tile of the navigation item", example = "Link 4")
  @NotBlank
  @Size(min=1, max = 20)
  private String title;

  @ApiModelProperty(value = "The URL of the navigation item", example = "https://www.google.de/")
  @NotNull
  private URL url;
}
