package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Schema
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSizeRequestDto {

  @Schema @NotNull @Valid public CohortDto cohortDto;

  @Schema @NotNull @NotEmpty
  public List<String> templateIds;
}
