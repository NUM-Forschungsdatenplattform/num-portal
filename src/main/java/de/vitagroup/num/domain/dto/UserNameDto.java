package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Schema
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNameDto {

  @NotNull
  @Schema(description = "The user's first name", example = "John")
  private String firstName;

  @NotNull
  @Schema(description = "The user's last name", example = "Doe")
  private String lastName;

}
