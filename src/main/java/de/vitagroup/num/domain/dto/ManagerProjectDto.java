package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema
public class ManagerProjectDto {

  @NotNull private CohortDto cohort;

  @NotNull @NotEmpty private List<String> templates;
}
