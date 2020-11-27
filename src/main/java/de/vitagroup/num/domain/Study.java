package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.MapConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Study {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String description;

  @Convert(converter = MapConverter.class)
  private Map<String, String> templates;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "cohort_id", referencedColumnName = "id")
  private Cohort cohort;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "coordinator_id")
  private UserDetails coordinator;

  @ManyToMany
  @JoinTable(
      name = "study_users",
      joinColumns = @JoinColumn(name = "study_id"),
      inverseJoinColumns = @JoinColumn(name = "user_details_id"))
  private List<UserDetails> researchers;
}
