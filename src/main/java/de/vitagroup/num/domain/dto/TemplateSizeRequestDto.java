package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
