package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.vitagroup.num.domain.StudyCategories;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
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

  @ApiModelProperty private String simpleDescription;

  @ApiModelProperty private boolean usedOutsideEu;

  @ApiModelProperty @Valid private List<TemplateInfoDto> templates;

  @ApiModelProperty private Long cohortId;

  @ApiModelProperty private User coordinator;

  @ApiModelProperty private List<UserDetailsDto> researchers;

  @ApiModelProperty private Set<String> keywords;

  @ApiModelProperty private Set<StudyCategories> categories;

  @ApiModelProperty
  @NotNull(message = "Study first hypothesis cannot be null")
  @NotEmpty(message = "Study first hypothesis cannot be empty")
  private String firstHypotheses;

  @ApiModelProperty private String secondHypotheses;

  @ApiModelProperty
  @NotNull(message = "Study goal cannot be null")
  @NotEmpty(message = "Study goal cannot be empty")
  private String goal;

  @NotNull(message = "Study status is mandatory")
  @ApiModelProperty
  private StudyStatus status;

  @ApiModelProperty private OffsetDateTime createDate;

  @ApiModelProperty private OffsetDateTime modifiedDate;

  @ApiModelProperty
  @NotNull(message = "Study startDate cannot be null")
  private LocalDate startDate;

  @ApiModelProperty
  @NotNull(message = "Study endDate cannot be null")
  private LocalDate endDate;

  @ApiModelProperty @Builder.Default private boolean financed = false;
}
