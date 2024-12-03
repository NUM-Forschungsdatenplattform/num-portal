package org.highmed.numportal.domain.model;

import org.highmed.numportal.domain.model.admin.UserDetails;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTransition implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  private ProjectStatus fromStatus;

  @Enumerated(EnumType.STRING)
  private ProjectStatus toStatus;

  private OffsetDateTime createDate;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "user_details_id")
  private UserDetails user;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "project_id")
  private Project project;

}
