package de.vitagroup.num.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aql implements Serializable {

  @ApiModelProperty(required = true, value = "The unique identifier", example = "1")
  @NotNull(message = "Id is mandatory")
  private Long id;

  @ApiModelProperty(required = true, value = "The AQL query")
  @NotBlank(message = "Query is mandatory")
  @NotNull(message = "Query is mandatory")
  private String query;

  @ApiModelProperty(required = true, value = "The name of the AQL query")
  @NotNull(message = "Name is mandatory")
  @NotBlank(message = "Name is mandatory")
  private String name;
}
