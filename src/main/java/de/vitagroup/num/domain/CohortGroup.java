package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import de.vitagroup.num.domain.repository.MapConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "children")
public class CohortGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Type type;
  private Operator operator;

  @Convert(converter = MapConverter.class)
  private Map<String, String> parameters;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "parent_group_id")
  private CohortGroup parent;

  @JsonManagedReference
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
  private Set<CohortGroup> children = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "phenotype_id", referencedColumnName = "id")
  private Phenotype phenotype;
}
