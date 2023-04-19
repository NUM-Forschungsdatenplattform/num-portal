package de.vitagroup.num.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SlimAqlDto {

  @NotNull(message = "Query cannot be null")
  @NotEmpty(message = "Query cannot be empty")
  private String query;

}
