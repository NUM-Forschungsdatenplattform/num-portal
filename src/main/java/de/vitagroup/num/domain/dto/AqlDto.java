package de.vitagroup.num.domain.dto;

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

  @ApiModelProperty
  private String use;

  @ApiModelProperty
  private String purpose;

  @ApiModelProperty(required = true, value = "The query string of the aql")
  @NotBlank(message = "AQL query should not be blank")
  @NotNull(message = "AQL query is mandatory")
  private String query;

  @ApiModelProperty(value = "Flag marking aql as being public")
  private boolean publicAql = true;

  @ApiModelProperty(value = "The user id of the owner of the aql")
  private String ownerId;

  @ApiModelProperty(value = "The organization id of the owner of the aql")
  private String organizationId;

  @ApiModelProperty private OffsetDateTime createDate;

  @ApiModelProperty private OffsetDateTime modifiedDate;
}
