package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Data;

@Data
@ApiModel
public class ManagerProjectDto {

  private CohortDto cohort;

  private List<String> templates;
}
