package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Schema
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDto {

  @NotNull
  @Schema(description = "The user id", example = "1")
  private String userId;

  @Schema(description = "The organization id", example = "12345")
  private String organizationId;

  @Schema(description = "User approval", example = "true")
  private Boolean approved;
}
