package org.highmed.numportal.domain.dto;

import org.highmed.numportal.domain.model.ProjectStatus;
import org.highmed.numportal.domain.model.admin.User;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectViewDto {

  private Long id;

  private String name;

  private User coordinator;

  private ProjectStatus status;

  private LocalDate startDate;

  private LocalDate endDate;

  private OffsetDateTime createDate;

}
