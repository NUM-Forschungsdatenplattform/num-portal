package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Dto for template metadata retrieved from ehr base */
@Data
@Builder
@Schema
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMetadataDto {

  @Schema(description = "The ehrbase template id")
  private String templateId;

  private String name;

  private String archetypeId;

  private OffsetDateTime createdOn;
}
