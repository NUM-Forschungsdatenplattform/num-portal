package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema
public class ManagerProjectDto {

  @NotNull private CohortDto cohort;

  @NotNull @NotEmpty private List<String> templates;
}
