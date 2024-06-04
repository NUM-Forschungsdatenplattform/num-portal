package org.highmed.numportal.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.highmed.numportal.attachment.domain.dto.AttachmentDto;
import org.highmed.numportal.domain.model.ProjectCategories;
import org.highmed.numportal.domain.model.ProjectStatus;
import org.highmed.numportal.domain.model.admin.User;

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

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
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

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  private OffsetDateTime createDate;

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  private OffsetDateTime modifiedDate;

  @NotNull(message = "Project startDate cannot be null")
  private LocalDate startDate;

  @NotNull(message = "Project endDate cannot be null")
  private LocalDate endDate;

  @Builder.Default private boolean financed = false;

  @Schema(description = "attachment's id to be deleted")
  private Set<Long> attachmentsToBeDeleted;

  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
  private List<String> filesDescription;

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "attachments assigned to project")
  private List<AttachmentDto> attachments;
}
