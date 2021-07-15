package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@ApiModel
@Data
@Builder
public class CohortSizeDto {

  @ApiModelProperty(value = "Count of patients in the cohort.")
  private int count;

  @ApiModelProperty(value = "The age distribution of the cohort.")
  private Map<String, Integer> ages;

  @ApiModelProperty(value = "The number of patients per hospital.")
  private Map<String, Integer> hospitals;
}
