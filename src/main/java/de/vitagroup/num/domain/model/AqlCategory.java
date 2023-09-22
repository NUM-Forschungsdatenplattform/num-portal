package de.vitagroup.num.domain.model;

import de.vitagroup.num.domain.repository.MapConverter;
import de.vitagroup.num.domain.validation.ValidTranslatedString;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AqlCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Convert(converter = MapConverter.class)
  @ValidTranslatedString
  Map<String, String> name;
}
