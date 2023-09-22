package de.vitagroup.num.domain.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cohort implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String description;

  @OneToOne(mappedBy = "cohort")
  private Project project;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "cohort_group_id", referencedColumnName = "id")
  private CohortGroup cohortGroup;
}
