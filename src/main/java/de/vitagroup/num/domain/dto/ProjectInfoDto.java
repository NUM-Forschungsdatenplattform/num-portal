package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectInfoDto {

  @ApiModelProperty(value = "Project create date")
  private OffsetDateTime createDate;

  @ApiModelProperty(value = "Project title")
  private String title;

  @ApiModelProperty(value = "Organization name")
  private String organization;

  @ApiModelProperty(value = "Coordinator name")
  private String coordinator;
}
