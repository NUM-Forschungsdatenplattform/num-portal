package org.highmed.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ProjectInfoDto {

  @Schema(description = "Project create date")
  private OffsetDateTime createDate;

  @Schema(description = "Project title")
  private String title;

  @Schema(description = "Organization name")
  private String organization;

  @Schema(description = "Coordinator name")
  private String coordinator;
}
