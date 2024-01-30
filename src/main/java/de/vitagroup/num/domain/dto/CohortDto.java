package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.validation.ValidCohort;
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
public class CohortDto {

  @Schema(description = "The unique identifier", example = "1")
  private Long id;

  @Schema(description = "The name of the cohort")
  @NotBlank(message = "Cohort name should not be blank")
  @NotNull(message = "Cohort name is mandatory")
  private String name;

  @Schema(description = "The description of the cohort")
  private String description;

  @Schema(description = "Reference to the project")
  @NotNull(message = "Id of the project is mandatory")
  private Long projectId;

  @Schema(description = "Aql cohort groups")
  @NotNull(message = "Cohort group is mandatory")
  @ValidCohort(message = "Invalid cohort group")
  private CohortGroupDto cohortGroup;
}
