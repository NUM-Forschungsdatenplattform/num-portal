package org.highmed.numportal.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.io.Serializable;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="maildomain")
public class MailDomain implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name="organization_id")
  private Organization organization;

}
