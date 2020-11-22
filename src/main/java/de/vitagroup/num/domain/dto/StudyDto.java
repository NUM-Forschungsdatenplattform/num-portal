package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
public class StudyDto {

  @ApiModelProperty @NotNull private Long id;

  @ApiModelProperty @NotNull @NotEmpty private String name;

  @ApiModelProperty private String description;

  @ApiModelProperty private List<TemplateInfoDto> templates;

  @ApiModelProperty private Long cohortId;
}
