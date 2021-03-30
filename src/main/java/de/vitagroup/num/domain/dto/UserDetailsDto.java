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
public class UserDetailsDto {

  @NotNull
  @ApiModelProperty(required = true, value = "The user id", example = "1")
  private String userId;

  @ApiModelProperty(value = "The organization id", example = "12345")
  private String organizationId;

  @ApiModelProperty(value = "User approval", example = "true")
  private Boolean approved;
}
