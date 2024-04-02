package org.highmed.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Schema
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

  @Schema(description = "The organization id", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  @Schema(description = "The name of the organization")
  @NotEmpty(message = "Organization name cannot be empty")
  @NotNull(message = "Organization name cannot be null")
  private String name;

  @Schema(description = "The list of mail domains attached to this organization")
  private Set<String> mailDomains;

  @Schema(description = "Flag used to mark if organization can be deleted if no users assigned", accessMode = Schema.AccessMode.READ_ONLY)
  private boolean allowedToBeDeleted;

  @Schema(description = "Flag used to mark if an organization is active or not")
  private Boolean active;
}
