package org.highmed.numportal.domain.model;

import org.highmed.numportal.domain.repository.MapConverter;
import org.highmed.numportal.domain.validation.ValidTranslatedString;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AqlCategory {

  @Convert(converter = MapConverter.class)
  @ValidTranslatedString
  Map<String, String> name;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
