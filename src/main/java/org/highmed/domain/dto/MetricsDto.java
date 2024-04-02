package org.highmed.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricsDto {

  @Schema(description = "Number of aqls")
  private long aqls;

  @Schema(description = "Number of projects")
  private long projects;

  @Schema(description = "Number of organizations")
  private long organizations;
}
