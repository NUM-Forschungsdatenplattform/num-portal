package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RawQueryDto {

  @Schema(description = "Raw aql query")
  @NotNull
  @NotEmpty
  private String query;
}
