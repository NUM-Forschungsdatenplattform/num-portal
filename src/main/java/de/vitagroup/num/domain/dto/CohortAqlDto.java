package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CohortAqlDto {

  @ApiModelProperty(value = "The unique identifier of the aql", example = "1")
  @NotNull
  private Long id;

  @ApiModelProperty(required = true, value = "The query string of the aql")
  @NotBlank(message = "AQL query should not be blank")
  @NotNull(message = "AQL query is mandatory")
  private String query;

  @ApiModelProperty(required = true, value = "The name of the aql query")
  @NotBlank(message = "AQL name should not be blank")
  @NotNull(message = "AQL name is mandatory")
  private String name;
}
