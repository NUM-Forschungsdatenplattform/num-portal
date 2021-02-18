package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.validation.ValidExpression;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
public class ExpressionDto {
  @NotNull(message = "Query is mandatory")
  @ValidExpression(message = "Invalid expression definition")
  @ApiModelProperty(required = true, value = "The aql query tree defining the phenotype")
  private Expression query;
}
