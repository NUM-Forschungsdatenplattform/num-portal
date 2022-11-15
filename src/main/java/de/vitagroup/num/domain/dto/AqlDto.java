package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.admin.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AqlDto {

  @Schema(description = "The unique identifier", example = "1")
  private Long id;

  @Schema(required = true, description = "The name of the aql")
  @NotBlank(message = "AQL name should not be blank")
  @NotNull(message = "AQL name is mandatory")
  private String name;

  @Schema(required = true)
  @NotBlank(message = "AQL use should not be blank")
  @NotNull(message = "AQL use is mandatory")
  private String use;

  @Schema(required = true)
  @NotBlank(message = "AQL purpose should not be blank")
  @NotNull(message = "AQL purpose is mandatory")
  private String purpose;

  @Schema(
      required = true,
      description = "The name of the aql, translated to a different language")
  @NotBlank(message = "AQL translated name should not be blank")
  @NotNull(message = "AQL translated name is mandatory")
  private String nameTranslated;

  @Schema(
      required = true,
      description = "The use of the aql, translated to a different language")
  @NotBlank(message = "AQL translated use should not be blank")
  @NotNull(message = "AQL translated use is mandatory")
  private String useTranslated;

  @Schema(
      required = true,
      description = "The purpose of the aql, translated to a different language")
  @NotBlank(message = "AQL translated purpose should not be blank")
  @NotNull(message = "AQL translated purpose is mandatory")
  private String purposeTranslated;

  @Schema(required = true, description = "The query string of the aql")
  @NotBlank(message = "AQL query should not be blank")
  @NotNull(message = "AQL query is mandatory")
  private String query;

  @Schema(description = "Flag marking aql as being public")
  private boolean publicAql = true;

  @Schema(description = "The owner of the aql", hidden = true)
  private User owner;

  @Schema(description = "The category of the aql")
  private Long categoryId;

  @Schema(description = "to for category data", hidden = true)
  private AqlCategoryDto category;

  @Schema private OffsetDateTime createDate;

  @Schema private OffsetDateTime modifiedDate;
}
