package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

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
