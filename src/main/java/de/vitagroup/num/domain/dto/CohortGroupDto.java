package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Type;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CohortGroupDto {

    @ApiModelProperty(
            value = "The unique identifier",
            example = "1")
    private Long id;

    @ApiModelProperty(
            value = "Cohort group operation to be applied to the children",
            example = "AND")
    private Operator operator;

    @ApiModelProperty(
            value = "Cohort group parameter map representing the name of the aql parameter and the corresponding")
    private Map<String, String> parameters;

    @ApiModelProperty(
            required = true,
            value = "Type of the cohort group",
            example = "PHENOTYPE")
    @NotNull(message = "Type cannot be null")
    private Type type;

    @ApiModelProperty(
            value = "Children of the cohort group in case the type of the group is: GROUP; can be other groups or phenotypes")
    private List<CohortGroupDto> children;

    @ApiModelProperty(
            value = "Reference to phenotype in case the type of the group is: PHENOTYPE",
            example = "2")
    private Long phenotypeId;

}
