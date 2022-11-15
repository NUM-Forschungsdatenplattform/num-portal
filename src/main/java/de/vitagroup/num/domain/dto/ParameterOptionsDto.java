package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema
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
