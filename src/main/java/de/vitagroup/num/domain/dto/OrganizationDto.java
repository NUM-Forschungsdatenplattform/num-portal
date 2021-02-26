package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

@Data
@ApiModel
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

  @NotNull
  @ApiModelProperty(required = true, value = "The organization external identifier", example = "1a")
  private String id;

  @ApiModelProperty(value = "The name of the organization")
  private String name;
}
