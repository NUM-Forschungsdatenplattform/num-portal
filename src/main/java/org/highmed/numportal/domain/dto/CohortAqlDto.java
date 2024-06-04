package org.highmed.numportal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CohortAqlDto {

  @Schema(description = "The unique identifier of the aql", example = "1")
  @NotNull
  private Long id;

  @Schema(description = "The query string of the aql")
  @NotBlank(message = "AQL query should not be blank")
  @NotNull(message = "AQL query is mandatory")
  private String query;

  @Schema(description = "The name of the aql query")
  @NotBlank(message = "AQL name should not be blank")
  @NotNull(message = "AQL name is mandatory")
  private String name;
}
