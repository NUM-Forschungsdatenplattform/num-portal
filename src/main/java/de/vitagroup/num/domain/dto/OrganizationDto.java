package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

@Data
@Schema
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

  @Schema(description = "The organization id", example = "1")
  private Long id;

  @Schema(description = "The name of the organization")
  @NotEmpty(message = "Organization name cannot be empty")
  @NotNull(message = "Organization name cannot be null")
  private String name;

  @Schema(description = "The list of mail domains attached to this organization")
  private Set<String> mailDomains;
}
