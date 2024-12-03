package org.highmed.numportal.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dto for template information linked to a project
 */
@Data
@Builder
@Schema
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateInfoDto {

  @NotNull(message = "Template id cannot be null")
  @NotEmpty(message = "Template id cannot be empty")
  @Schema(description = "The ehrbase identifier of the template")
  private String templateId;

  @NotNull(message = "Template name cannot be null")
  @NotEmpty(message = "Template name cannot be empty")
  private String name;
}
