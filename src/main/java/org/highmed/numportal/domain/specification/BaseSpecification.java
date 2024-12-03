package org.highmed.numportal.domain.specification;

import org.highmed.numportal.domain.dto.Language;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

@SuperBuilder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BaseSpecification {

  protected Map<String, ?> filter;

  @Nonnull
  protected String loggedInUserId;

  protected Long loggedInUserOrganizationId;

  protected Language language;

  protected Sort.Order sortOrder;

  protected Set<String> ownersUUID;
}
