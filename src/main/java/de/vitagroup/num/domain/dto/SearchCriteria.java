package de.vitagroup.num.domain.dto;

import de.vitagroup.num.web.exception.BadRequestException;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Data
@Builder
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {

    private Map<String, ?> filter;

    private String sort;

    private String sortBy;

    public boolean isValid() {
        if (StringUtils.isEmpty(this.sort) && StringUtils.isNotEmpty(this.sortBy)) {
            throw new BadRequestException("Sort field is required when sortBy is provided");
        }
        if (StringUtils.isNotEmpty(this.sort) && StringUtils.isEmpty(this.sortBy)) {
            throw new BadRequestException("sortBy field is required when sort is provided");
        }
        return true;
    }
}
