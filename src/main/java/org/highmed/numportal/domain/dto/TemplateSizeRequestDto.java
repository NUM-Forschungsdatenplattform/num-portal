package org.highmed.numportal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@Schema
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSizeRequestDto {

  @NotNull @Valid public CohortDto cohortDto;

  @NotNull @NotEmpty
  public List<String> templateIds;
}
