package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricsDto {

  @ApiModelProperty(value = "Number of aqls")
  private long aqls;

  @ApiModelProperty(value = "Number of projects")
  private long projects;

  @ApiModelProperty(value = "Number of organizations")
  private long organizations;
}
