package org.highmed.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.highmed.domain.model.CohortAql;
import org.highmed.domain.model.Operator;
import org.highmed.domain.model.Type;
import org.highmed.domain.repository.AqlConverter;
import org.highmed.domain.repository.MapConverter;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "children")
public class CohortGroup implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Type type;
  private Operator operator;

  @Convert(converter = MapConverter.class)
  private Map<String, Object> parameters;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "parent_group_id")
  private CohortGroup parent;

  @JsonManagedReference
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
  private List<CohortGroup> children = new LinkedList<>();

  @Convert(converter = AqlConverter.class)
  private CohortAql query;
}
