package de.vitagroup.num.domain.model.admin;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.vitagroup.num.domain.model.Organization;
import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
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

  @Id
  private String userId;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Builder.Default
  private boolean approved = false;

  private LocalDateTime createdDate;

  public boolean isNotApproved() {
    return !approved;
  }

  public UserDetails(String userId, Organization organization, boolean approved) {
    this.userId = userId;
    this.organization = organization;
    this.approved = approved;
  }

}
