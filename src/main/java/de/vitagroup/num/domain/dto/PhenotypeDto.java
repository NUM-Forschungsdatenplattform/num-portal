package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.validation.ValidExpression;
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
public class PhenotypeDto {

  @ApiModelProperty(value = "The unique identifier", example = "1")
  private Long id;

  @ApiModelProperty(required = true, value = "The name of the phenotype")
  @NotBlank(message = "Phenotype name cannot be blank")
  @NotNull(message = "Phenotype name is mandatory")
  private String name;

  @ApiModelProperty(required = true, value = "The description of the phenotype")
  @NotBlank(message = "Phenotype description cannot be blank")
  private String description;

  @NotNull(message = "Query is mandatory")
  @ValidExpression(message = "Invalid phenotype definition")
  @ApiModelProperty(required = true, value = "The aql query tree defining the phenotype")
  private Expression query;

  @ApiModelProperty private Long ownerId;
}
