package org.highmed.numportal.domain.specification;

import org.highmed.numportal.domain.dto.Language;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

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
