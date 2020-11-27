package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dto for template information linked to a study */
@Data
@Builder
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateInfoDto {

  @ApiModelProperty(value = "The ehrbase identifier of the template")
  private String id;

  @ApiModelProperty private String name;
}
