package org.highmed.numportal.domain.dto;

import org.highmed.numportal.service.exception.BadRequestException;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Data
@Builder
@Schema
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {

  public static final String FILTER_SEARCH_BY_KEY = "search";

  public static final String FILTER_BY_TYPE_KEY = "type";

  // boolean flag used for users search/filter (optional -> omitting it returns both)
  public static final String FILTER_APPROVED_KEY = "approved";

  // boolean flag used for users search/filter (optional)
  public static final String FILTER_USER_WITH_ROLES_KEY = "withRoles";
  public static final String FILTER_BY_STATUS = "status";
  public static final String FILTER_BY_ROLES = "roles";
  public static final String FILTER_BY_ACTIVE = "enabled";
  public static final String FILTER_BY_ACTIVE_ORGANIZATION = "active";
  private static final String AUTHOR_NAME = "author";
  private Map<String, ? super String> filter;

  private String sort;

  private String sortBy;

  private Language language;

  public boolean isValid() {
    if (StringUtils.isEmpty(this.sort) && StringUtils.isNotEmpty(this.sortBy)) {
      throw new BadRequestException(SearchCriteria.class, "sort field is required when sortBy is provided");
    }
    if (StringUtils.isNotEmpty(this.sort) && StringUtils.isEmpty(this.sortBy)) {
      throw new BadRequestException(SearchCriteria.class, "sortBy field is required when sort is provided");
    }
    return true;
  }

  public boolean isSortByAuthor() {
    return AUTHOR_NAME.equals(this.getSortBy());
  }
}
