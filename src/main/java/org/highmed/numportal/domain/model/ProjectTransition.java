package org.highmed.numportal.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.highmed.numportal.domain.model.admin.UserDetails;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
