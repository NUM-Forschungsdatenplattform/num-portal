package de.vitagroup.num.domain.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "domains")
public class Organization implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @ToString.Exclude
  @JsonManagedReference
  @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<MailDomain> domains = new HashSet<>();

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
  private Boolean active = Boolean.TRUE;
}
