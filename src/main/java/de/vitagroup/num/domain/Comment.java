package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.vitagroup.num.domain.admin.UserDetails;
import java.io.Serializable;
import java.time.OffsetDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
public class Comment implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String text;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "author_id")
  private UserDetails author;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "study_id")
  private Study study;

  private OffsetDateTime createDate;

  public boolean hasEmptyOrDifferentAuthor(String userId) {
    return ObjectUtils.isEmpty(author) || !author.getUserId().equals(userId);
  }
}
