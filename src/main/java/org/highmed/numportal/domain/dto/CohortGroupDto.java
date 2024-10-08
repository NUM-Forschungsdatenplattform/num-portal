package org.highmed.numportal.domain.dto;

import org.highmed.numportal.domain.model.Operator;
import org.highmed.numportal.domain.model.Type;
import org.highmed.numportal.domain.repository.AqlConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CohortGroupDto {

  @Schema(description = "The unique identifier", example = "1")
  private Long id;

  @Schema(description = "Cohort group operation to be applied to the children", example = "AND")
  private Operator operator;

  @Schema(
      description =
          "Cohort group parameter map representing the name of the aql parameter and the corresponding value")
  private Map<String, Object> parameters;

  @Schema(description = "Type of the cohort group", example = "AQL")
  @NotNull(message = "Type cannot be null")
  private Type type;

  @Schema(description =
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
