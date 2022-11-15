package de.vitagroup.num.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNameDto {

  @NotNull
  @Schema(required = true, description = "The user's first name", example = "John")
  private String firstName;

  @NotNull
  @Schema(required = true, description = "The user's last name", example = "Doe")
  private String lastName;

}
