package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterOptionsDto {

  private Map<String, Object> options = new LinkedHashMap<>();
  private String aqlPath;
  private String archetypeId;
  private String type;
  private String unit;
}
