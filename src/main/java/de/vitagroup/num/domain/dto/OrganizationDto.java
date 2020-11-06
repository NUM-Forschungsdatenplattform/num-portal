package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class OrganizationDto {

  @NotNull
  @ApiModelProperty(required = true, value = "The organization external identifier", example = "1a")
  private String id;

  @ApiModelProperty(required = true, value = "The name of the organization")
  private String name;
}
