package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Schema
@Data
@Builder
public class CohortSizeDto {

  @Schema(description = "Count of patients in the cohort.")
  private int count;

  @Schema(description = "The age distribution of the cohort.")
  private Map<String, Integer> ages;

  @Schema(description = "The number of patients per hospital.")
  private Map<String, Integer> hospitals;
}
