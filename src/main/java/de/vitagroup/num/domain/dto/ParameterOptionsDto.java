package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import java.util.HashMap;
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

  private HashMap<String, Object> options = new HashMap<>();
  private String aqlPath;
  private String archetypeId;
  private String type;
  private String unit;
}
