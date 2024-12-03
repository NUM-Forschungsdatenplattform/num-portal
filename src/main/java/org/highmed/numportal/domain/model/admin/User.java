package org.highmed.numportal.domain.model.admin;

import org.highmed.numportal.domain.dto.OrganizationDto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  // keycloak attribute for user's active field - if set to false user is not allowed to log in
  private Boolean enabled;

  @JsonIgnore
  public boolean isNotApproved() {
    return !approved;
  }

  @JsonIgnore
  public String getFullName() {
    return this.firstName + " " + this.lastName;
  }
}
