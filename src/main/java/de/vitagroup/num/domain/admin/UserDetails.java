package de.vitagroup.num.domain.admin;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.vitagroup.num.domain.Organization;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "organization_id")
  private Organization organization;

  private boolean approved = false;

  public boolean isNotApproved() {
    return !approved;
  }
}
