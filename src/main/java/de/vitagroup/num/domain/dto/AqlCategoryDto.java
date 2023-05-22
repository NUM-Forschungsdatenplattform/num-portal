package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.validation.ValidTranslatedString;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AqlCategoryDto {
  @Schema(description = "The unique identifier", example = "1")
  private Long id;

  @Schema(description = "The map of language text pairs", example = "{'en':'text in english','de':'text auf deutsch'}")
  @ValidTranslatedString(message = "Translated string must contain at lest 'en' and 'de' translations.")
  Map<String, String> name;
}
