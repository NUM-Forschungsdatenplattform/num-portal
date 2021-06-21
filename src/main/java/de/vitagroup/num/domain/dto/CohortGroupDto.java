package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.repository.AqlConverter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.persistence.Convert;
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
public class CohortGroupDto {

  @ApiModelProperty(value = "The unique identifier", example = "1")
  private Long id;

  @ApiModelProperty(value = "Cohort group operation to be applied to the children", example = "AND")
  private Operator operator;

  @ApiModelProperty(
      value =
          "Cohort group parameter map representing the name of the aql parameter and the corresponding value")
  private Map<String, Object> parameters;

  @ApiModelProperty(required = true, value = "Type of the cohort group", example = "PHENOTYPE")
  @NotNull(message = "Type cannot be null")
  private Type type;

  @ApiModelProperty(
      value =
          "Children of the cohort group in case the type of the group is: GROUP; can be other groups or aqls")
  private List<CohortGroupDto> children;

  @Convert(converter = AqlConverter.class)
  private CohortAqlDto query;

  @JsonIgnore
  public boolean isAql() {
    return Type.AQL == type;
  }

  @JsonIgnore
  public boolean isGroup() {
    return Type.GROUP == type;
  }
}
