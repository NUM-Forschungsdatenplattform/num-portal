package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URL;

@Data
@Schema
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemDto {

  @Schema(description = "The tile of the navigation item", example = "Link 4")
  @NotBlank
  @Size(min=1, max = 20)
  private String title;

  @Schema(description = "The URL of the navigation item", example = "https://www.google.de/")
  @NotNull
  private URL url;
}
