package org.highmed.numportal.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.highmed.numportal.domain.model.admin.UserDetails;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.persistence.*;
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
  @JoinColumn(name = "project_id")
  private Project project;

  private OffsetDateTime createDate;

  public boolean hasEmptyOrDifferentAuthor(String userId) {
    return ObjectUtils.isEmpty(author) || !author.getUserId().equals(userId);
  }
}
