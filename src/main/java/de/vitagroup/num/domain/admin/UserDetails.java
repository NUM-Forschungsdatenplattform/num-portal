package de.vitagroup.num.domain.admin;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetails {

  @Id private String userId;

  private String organizationId;

  private Boolean approved = false;

  public boolean isApproved() {
    return approved;
  }

  public boolean isNotApproved() {
    return !approved;
  }
}
