package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

@Data
@ApiModel
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

  @ApiModelProperty(required = true, value = "The organization id", example = "1")
  private Long id;

  @ApiModelProperty(value = "The name of the organization")
  @NotEmpty(message = "Organization name cannot be empty")
  @NotNull(message = "Organization name cannot be null")
  private String name;

  @ApiModelProperty(value = "The list of mail domains attached to this organization")
  private Set<String> mailDomains;
}
