package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.ExpressionConverter;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import javax.persistence.Convert;
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
@ApiModel
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Phenotype implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String description;

  private boolean deleted;

  @Convert(converter = ExpressionConverter.class)
  private Expression query;

  @ManyToOne
  @JsonBackReference
  @JoinColumn(name = "owner_id")
  private UserDetails owner;

  public boolean hasEmptyOrDifferentOwner(String userId) {
    return ObjectUtils.isEmpty(owner) || !owner.getUserId().equals(userId);
  }
}
