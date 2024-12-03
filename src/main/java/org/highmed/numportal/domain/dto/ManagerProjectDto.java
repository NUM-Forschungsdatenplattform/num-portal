package org.highmed.numportal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema
public class ManagerProjectDto {

  @NotNull
  private CohortDto cohort;

  @NotNull
  @NotEmpty
  private List<String> templates;
}
