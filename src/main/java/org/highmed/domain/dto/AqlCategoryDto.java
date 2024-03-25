package org.highmed.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.highmed.domain.validation.ValidTranslatedString;

import java.util.Map;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AqlCategoryDto {
  @Schema(description = "The unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  @Schema(description = "The map of language text pairs", example = "{'en':'text in english','de':'text auf deutsch'}")
  @ValidTranslatedString(message = "Translated string must contain at lest 'en' and 'de' translations.")
  Map<String, String> name;

  @Schema(description = "Flag used to mark if category can be deleted if no aqls assigned", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean allowedToBeDeleted;
}
