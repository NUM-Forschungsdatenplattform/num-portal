package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/** Dto for template information linked to a study */
@Data
@Builder
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateInfoDto {

  @NotNull(message = "Template id cannot be null")
  @NotEmpty(message = "Template id cannot be empty")
  @ApiModelProperty(value = "The ehrbase identifier of the template")
  private String templateId;

  @NotNull(message = "Template name cannot be null")
  @NotEmpty(message = "Template name cannot be empty")
  @ApiModelProperty private String name;
}
