package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.validation.ValidTranslatedString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AqlCategoryDto {
  @ApiModelProperty(value = "The unique identifier", example = "1")
  private Long id;

  @ApiModelProperty(value = "The map of language text pairs", example = "{'en':'text in english','de':'text auf deutsch'}")
  @ValidTranslatedString(message = "Translated string must contain at lest 'en' and 'de' translations.")
  Map<String, String> name;
}
