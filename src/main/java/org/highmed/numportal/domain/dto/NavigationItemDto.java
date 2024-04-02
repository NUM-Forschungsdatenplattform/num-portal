package org.highmed.numportal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
