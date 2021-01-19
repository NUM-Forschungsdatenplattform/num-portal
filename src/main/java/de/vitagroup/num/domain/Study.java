package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.vitagroup.num.domain.repository.MapConverter;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.web.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.time.OffsetDateTime;
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

  private String firstHypotheses;

  private String secondHypotheses;

  private StudyStatus status;

  private OffsetDateTime createDate;

  private OffsetDateTime modifiedDate;

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

  public void setStatus(StudyStatus status) {
    validateStatus(status);
    this.status = status;
  }

  private void validateStatus(StudyStatus status) {

    if (status == null) {
      throw new BadRequestException("Invalid study status");
    }

    if (this.status == null) {
      if (!isValidInitialStatus(status)) {
        throw new BadRequestException("Invalid study status: " + status);
      }
    } else if (!this.status.nextStates().contains(status)) {
      throw new BadRequestException(
          "Study status transition from " + this.status + " to " + status + " not allowed");
    }
  }

  private boolean isValidInitialStatus(StudyStatus status) {
    return status.equals(StudyStatus.DRAFT) || status.equals(StudyStatus.PENDING);
  }
}
