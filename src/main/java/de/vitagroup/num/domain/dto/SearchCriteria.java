package de.vitagroup.num.domain.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {

    private Map<String, ?> filter;
}
