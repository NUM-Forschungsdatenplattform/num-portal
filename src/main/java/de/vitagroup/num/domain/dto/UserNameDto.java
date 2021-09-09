package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNameDto {

  @NotNull
  @ApiModelProperty(required = true, value = "The user's first name", example = "John")
  private String firstName;

  @NotNull
  @ApiModelProperty(required = true, value = "The user's last name", example = "Doe")
  private String lastName;

}
