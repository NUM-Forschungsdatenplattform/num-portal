package de.vitagroup.num.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import de.vitagroup.num.domain.model.admin.UserDetails;
import de.vitagroup.num.domain.repository.CategorySetConverter;
import de.vitagroup.num.domain.repository.MapConverter;
import de.vitagroup.num.domain.repository.StringSetConverter;
import lombok.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"cohort", "transitions","translations"})
public class Project implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String description;

  private String simpleDescription;

  private boolean usedOutsideEu;

  private String firstHypotheses;

  private String secondHypotheses;

  private String goal;

  @Enumerated(EnumType.STRING)
  private ProjectStatus status;

  @Convert(converter = CategorySetConverter.class)
  private Set<ProjectCategories> categories;

  @Convert(converter = StringSetConverter.class)
  private Set<String> keywords;

  private OffsetDateTime createDate;

  private OffsetDateTime modifiedDate;

  private LocalDate startDate;

  private LocalDate endDate;

  private boolean financed;

  @Convert(converter = MapConverter.class)
  private Map<String, String> templates;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "cohort_id", referencedColumnName = "id")
  private Cohort cohort;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "coordinator_id")
  private UserDetails coordinator;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "project_users",
      joinColumns = @JoinColumn(name = "project_id"),
      inverseJoinColumns = @JoinColumn(name = "user_details_id"))
  private List<UserDetails> researchers;

  @JsonIgnore
  @ToString.Exclude
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumnOrFormula(column = @JoinColumn(name = "property", referencedColumnName = "status", insertable = false, updatable = false))
  @NotFound(action = NotFoundAction.IGNORE)
  private Set<Translation> translations = new HashSet<>();

  @ToString.Exclude
  @JsonManagedReference
  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ProjectTransition> transitions = new HashSet<>();

  public Project(Long id, String name, OffsetDateTime createDate, UserDetails coordinator) {
    this.id = id;
    this.name = name;
    this.createDate = createDate;
    this.coordinator = coordinator;
  }

  public boolean hasEmptyOrDifferentOwner(String userId) {
    return ObjectUtils.isEmpty(coordinator) || !coordinator.getUserId().equals(userId);
  }

  public boolean isProjectResearcher(String userId) {
    if (CollectionUtils.isEmpty(researchers)) {
      return false;
    }
    return researchers.stream().anyMatch(r -> userId.equals(r.getUserId()));
  }

  public boolean isCoordinator(String userId) {
    return coordinator != null && coordinator.getUserId().equals(userId);
  }

  public boolean isDeletable() {
    return ProjectStatus.DRAFT.equals(status) || ProjectStatus.CHANGE_REQUEST.equals(status);
  }
}
