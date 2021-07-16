package de.vitagroup.num.domain;

import de.vitagroup.num.domain.repository.MapConverter;
import de.vitagroup.num.domain.validation.ValidTranslatedString;
import java.util.Map;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
