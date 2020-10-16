package de.vitagroup.num.domain;

import de.vitagroup.num.domain.validation.ValidExpression;
import de.vitagroup.num.domain.repository.ExpressionConverter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@ApiModel
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Phenotype {
    @ApiModelProperty(
            required = true,
            value = "The unique identifier",
            example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(
            required = true,
            value = "The name of the phenotype")
    @NotBlank(message = "Phenotype name is mandatory")
    private String name;

    @ApiModelProperty(
            required = true,
            value = "The description of the phenotype")
    @NotBlank(message = "Phenotype description is mandatory")
    private String description;

    @ApiModelProperty(
            required = true,
            value = "The aql query tree defining the phenotype")
    @Convert(converter = ExpressionConverter.class)
    @NotNull(message = "Query is mandatory")
    @ValidExpression(message = "Invalid phenotype query")
    private Expression query;

}
