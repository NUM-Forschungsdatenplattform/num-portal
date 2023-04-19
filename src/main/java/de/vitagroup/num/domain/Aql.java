package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.vitagroup.num.domain.admin.UserDetails;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aql implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String use;

  private String purpose;

  private String nameTranslated;

  private String useTranslated;

  private String purposeTranslated;

  private String query;

  private boolean publicAql;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "owner_id")
  private UserDetails owner;

  private OffsetDateTime createDate;

  private OffsetDateTime modifiedDate;

  @ManyToOne
  @JoinColumn(name = "category_id", referencedColumnName = "id")
  private AqlCategory category;

  public boolean hasEmptyOrDifferentOwner(String userId) {
    return ObjectUtils.isEmpty(owner) || !owner.getUserId().equals(userId);
  }

  public boolean isExecutable(String userId) {
    return !ObjectUtils.isEmpty(owner) && (owner.getUserId().equals(userId) || isPublicAql());
  }

  public boolean isViewable(String userId) {
    return !ObjectUtils.isEmpty(owner) && (owner.getUserId().equals(userId) || isPublicAql());
  }
}
