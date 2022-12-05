package de.vitagroup.num.domain.dto;

import de.vitagroup.num.service.exception.BadRequestException;
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

    private Map<String, ?> filter;

    private String sort;

    private String sortBy;

    private String language;

    public boolean isValid() {
        if (StringUtils.isEmpty(this.sort) && StringUtils.isNotEmpty(this.sortBy)) {
            throw new BadRequestException(SearchCriteria.class, "sort field is required when sortBy is provided");
        }
        if (StringUtils.isNotEmpty(this.sort) && StringUtils.isEmpty(this.sortBy)) {
            throw new BadRequestException(SearchCriteria.class, "sortBy field is required when sort is provided");
        }
        return true;
    }
}
