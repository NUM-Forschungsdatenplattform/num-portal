package de.vitagroup.num.domain.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.vitagroup.num.domain.dto.OrganizationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

@Data
@Schema
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @NotNull
  @NotEmpty
  @Schema(
      description = "The external user identifier provided by the identity provider",
      example = "1")
  private String id;

  @NotNull
  @NotEmpty
  @Schema(description = "The username of the user")
  private String username;

  @NotNull
  @NotEmpty
  @Schema(description = "The first name of the user")
  private String firstName;

  @NotNull
  @NotEmpty
  @Schema(description = "The last name of the user")
  private String lastName;

  @NotNull
  @NotEmpty
  @Schema(description = "The email address of the user")
  private String email;

  @NotNull
  @NotEmpty
  @Schema(description = "The timestamp of user registration")
  private Long createdTimestamp;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Set<String> roles;

  @Schema
  private boolean approved;

  @Schema(description = "The organization")
  private OrganizationDto organization;

  private Boolean emailVerified;

  private Map<String, Object> attributes;

  @JsonIgnore
  public boolean isNotApproved() {
    return !approved;
  }

  @JsonIgnore
  public String getFullName() {
    return this.firstName + " " + this.lastName;
  }
}
