package org.highmed.numportal.domain.model.admin;

import org.highmed.numportal.domain.model.Organization;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

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

  public UserDetails(String userId, Organization organization, boolean approved) {
    this.userId = userId;
    this.organization = organization;
    this.approved = approved;
  }

  public boolean isNotApproved() {
    return !approved;
  }

}
