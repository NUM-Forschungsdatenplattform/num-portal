package org.highmed.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.highmed.domain.model.admin.User;

import java.time.OffsetDateTime;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AqlDto {

  @Schema(description = "The unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The name of the aql")
  @NotBlank(message = "AQL name should not be blank")
  @NotNull(message = "AQL name is mandatory")
  private String name;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "AQL use should not be blank")
  @NotNull(message = "AQL use is mandatory")
  private String use;

  @NotBlank(message = "AQL purpose should not be blank")
  @NotNull(message = "AQL purpose is mandatory")
  private String purpose;

  @Schema(description = "The name of the aql, translated to a different language")
  @NotBlank(message = "AQL translated name should not be blank")
  @NotNull(message = "AQL translated name is mandatory")
  private String nameTranslated;

  @Schema(description = "The use of the aql, translated to a different language")
  @NotBlank(message = "AQL translated use should not be blank")
  @NotNull(message = "AQL translated use is mandatory")
  private String useTranslated;

  @Schema(description = "The purpose of the aql, translated to a different language")
  @NotBlank(message = "AQL translated purpose should not be blank")
  @NotNull(message = "AQL translated purpose is mandatory")
  private String purposeTranslated;

  @Schema(description = "The query string of the aql")
  @NotBlank(message = "AQL query should not be blank")
  @NotNull(message = "AQL query is mandatory")
  private String query;

  @Schema(description = "Flag marking aql as being public")
  @Builder.Default
  private boolean publicAql = true;

  @Schema(description = "The owner of the aql", hidden = true)
  private User owner;

  @Schema(description = "The category of the aql")
  private Long categoryId;

  @Schema(description = "to for category data", hidden = true)
  private AqlCategoryDto category;

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  private OffsetDateTime createDate;

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  private OffsetDateTime modifiedDate;
}
