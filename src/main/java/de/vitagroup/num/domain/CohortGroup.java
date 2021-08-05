package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import de.vitagroup.num.domain.repository.AqlConverter;
import de.vitagroup.num.domain.repository.MapConverter;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
