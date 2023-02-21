package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.admin.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AqlDto {

  @ApiModelProperty(value = "The unique identifier", example = "1")
  private Long id;

  @ApiModelProperty(required = true, value = "The name of the aql")
  @NotBlank(message = "AQL name should not be blank")
  @NotNull(message = "AQL name is mandatory")
  private String name;

  @ApiModelProperty(required = true)
  @NotBlank(message = "AQL use should not be blank")
  @NotNull(message = "AQL use is mandatory")
  private String use;

  @ApiModelProperty(required = true)
  @NotBlank(message = "AQL purpose should not be blank")
  @NotNull(message = "AQL purpose is mandatory")
  private String purpose;

  @ApiModelProperty(
      required = true,
      value = "The name of the aql, translated to a different language")
  @NotBlank(message = "AQL translated name should not be blank")
  @NotNull(message = "AQL translated name is mandatory")
  private String nameTranslated;

  @ApiModelProperty(
      required = true,
      value = "The use of the aql, translated to a different language")
  @NotBlank(message = "AQL translated use should not be blank")
  @NotNull(message = "AQL translated use is mandatory")
  private String useTranslated;

  @ApiModelProperty(
      required = true,
      value = "The purpose of the aql, translated to a different language")
  @NotBlank(message = "AQL translated purpose should not be blank")
  @NotNull(message = "AQL translated purpose is mandatory")
  private String purposeTranslated;

  @ApiModelProperty(required = true, value = "The query string of the aql")
  @NotBlank(message = "AQL query should not be blank")
  @NotNull(message = "AQL query is mandatory")
  private String query;

  @ApiModelProperty(value = "Flag marking aql as being public")
  private boolean publicAql = true;

  @ApiModelProperty(value = "The owner of the aql", hidden = true)
  private User owner;

  @ApiModelProperty(value = "The category of the aql")
  private Long categoryId;

  @ApiModelProperty(value = "to for category data", hidden = true)
  private AqlCategoryDto category;

  @ApiModelProperty private OffsetDateTime createDate;

  @ApiModelProperty private OffsetDateTime modifiedDate;
}
