package de.vitagroup.num.domain.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class User {

  @NotNull
  @NotEmpty
  @ApiModelProperty(
      required = true,
      value = "The external user identifier provided by the identity provider",
      example = "1")
  private String id;

  @NotNull
  @NotEmpty
  @ApiModelProperty(required = true, value = "The username of the user")
  private String username;

  @NotNull
  @NotEmpty
  @ApiModelProperty(required = true, value = "The first name of the user")
  private String firstName;

  @NotNull
  @NotEmpty
  @ApiModelProperty(required = true, value = "The last name of the user")
  private String lastName;

  @NotNull
  @NotEmpty
  @ApiModelProperty(required = true, value = "The email address of the user")
  private String email;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Set<String> roles;

  @ApiModelProperty private boolean approved;

  @ApiModelProperty(value = "The external organization id from the terminology server")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String externalOrganizationId;

  @JsonIgnore
  public boolean isNotApproved() {
    return !approved;
  }
}
