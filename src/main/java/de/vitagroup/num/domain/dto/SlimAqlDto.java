package de.vitagroup.num.domain.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlimAqlDto {

  @NotNull(message = "Query cannot be null")
  @NotEmpty(message = "Query cannot be empty")
  private String query;

}
