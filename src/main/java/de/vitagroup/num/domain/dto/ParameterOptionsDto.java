package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import java.util.List;
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

  List<Object> options;
  String aqlPath;
  String archetypeId;
}
