package org.highmed.domain.model;

import org.highmed.domain.repository.MapConverter;
import org.highmed.domain.validation.ValidTranslatedString;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
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
