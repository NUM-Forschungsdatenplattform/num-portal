package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.vitagroup.num.domain.model.ProjectCategories;
import de.vitagroup.num.domain.model.ProjectStatus;
import de.vitagroup.num.domain.model.admin.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@Schema
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectDto {

  private Long id;

  @NotNull(message = "Project name cannot be null")
  @NotEmpty(message = "Project name cannot be empty")
  private String name;

  private String description;

  private String simpleDescription;

  private boolean usedOutsideEu;

  @Valid private List<TemplateInfoDto> templates;

  private Long cohortId;

  private User coordinator;

  private List<UserDetailsDto> researchers;

  private Set<String> keywords;

  private Set<ProjectCategories> categories;

  @NotNull(message = "Project first hypothesis cannot be null")
  @NotEmpty(message = "Project first hypothesis cannot be empty")
  private String firstHypotheses;

  private String secondHypotheses;

  @NotNull(message = "Project goal cannot be null")
  @NotEmpty(message = "Project goal cannot be empty")
  private String goal;

  @NotNull(message = "Project status is mandatory")
  private ProjectStatus status;

  private OffsetDateTime createDate;

  private OffsetDateTime modifiedDate;

  @NotNull(message = "Project startDate cannot be null")
  private LocalDate startDate;

  @NotNull(message = "Project endDate cannot be null")
  private LocalDate endDate;

  @Builder.Default private boolean financed = false;

  private Set<Long> attachmentsToBeDeleted;
}
