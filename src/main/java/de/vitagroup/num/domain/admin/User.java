package de.vitagroup.num.domain.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import lombok.Data;

@Data
public class User {

  private String id;
  private String userName;
  private String firstName;
  private String lastName;
  private String email;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Set<String> roles;
}
