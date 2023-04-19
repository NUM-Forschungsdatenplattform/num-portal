package de.vitagroup.num.domain.admin;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.vitagroup.num.domain.Organization;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetails implements Serializable {

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
