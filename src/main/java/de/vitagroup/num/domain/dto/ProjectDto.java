package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.vitagroup.num.domain.ProjectCategories;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.admin.User;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectDto {

  @Schema private Long id;

  @Schema
  @NotNull(message = "Project name cannot be null")
  @NotEmpty(message = "Project name cannot be empty")
  private String name;

  @Schema private String description;

  @Schema private String simpleDescription;

  @Schema private boolean usedOutsideEu;

  @Schema @Valid private List<TemplateInfoDto> templates;

  @Schema private Long cohortId;

  @Schema private User coordinator;

  @Schema private List<UserDetailsDto> researchers;

  @Schema private Set<String> keywords;

  @Schema private Set<ProjectCategories> categories;

  @Schema
  @NotNull(message = "Project first hypothesis cannot be null")
  @NotEmpty(message = "Project first hypothesis cannot be empty")
  private String firstHypotheses;

  @Schema private String secondHypotheses;

  @Schema
  @NotNull(message = "Project goal cannot be null")
  @NotEmpty(message = "Project goal cannot be empty")
  private String goal;

  @NotNull(message = "Project status is mandatory")
  @Schema
  private ProjectStatus status;

  @Schema private OffsetDateTime createDate;

  @Schema private OffsetDateTime modifiedDate;

  @Schema
  @NotNull(message = "Project startDate cannot be null")
  private LocalDate startDate;

  @Schema
  @NotNull(message = "Project endDate cannot be null")
  private LocalDate endDate;

  @Schema @Builder.Default private boolean financed = false;
}
