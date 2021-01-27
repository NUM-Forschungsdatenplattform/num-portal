package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.vitagroup.num.domain.StudyStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudyDto {

  @ApiModelProperty private Long id;

  @ApiModelProperty
  @NotNull(message = "Study name cannot be null")
  @NotEmpty(message = "Study name cannot be empty")
  private String name;

  @ApiModelProperty private String description;

  @ApiModelProperty
  @Valid
  private List<TemplateInfoDto> templates;

  @ApiModelProperty private Long cohortId;

  @ApiModelProperty private UserDetailsDto coordinator;

  @ApiModelProperty private List<UserDetailsDto> researchers;

  @ApiModelProperty
  @NotNull(message = "Study first hypotheses cannot be null")
  @NotEmpty(message = "Study first hypotheses cannot be empty")
  private String firstHypotheses;

  @ApiModelProperty private String secondHypotheses;

  @NotNull(message = "Study status is mandatory")
  @ApiModelProperty
  private StudyStatus status;

  @ApiModelProperty private OffsetDateTime createDate;

  @ApiModelProperty private OffsetDateTime modifiedDate;
}
