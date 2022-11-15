package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dto for template metadata retrieved from ehr base */
@Data
@Builder
@Schema
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMetadataDto {

  @Schema(description = "The ehrbase template id")
  private String templateId;

  @Schema private String name;

  @Schema private String archetypeId;

  @Schema private OffsetDateTime createdOn;
}
