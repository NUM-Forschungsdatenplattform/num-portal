package de.vitagroup.num.domain.dto;

import com.sun.istack.NotNull;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RawQueryDto {

  @ApiModelProperty(required = true, value = "Raw aql query")
  @NotNull
  @NotEmpty
  private String query;
}
