package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@ApiModel
public class ManagerProjectDto {

  @NotNull private CohortDto cohort;

  @NotNull @NotEmpty private List<String> templates;
}
