package org.highmed.numportal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema
public class QueryDto {
    @NotNull private String aql;
}
