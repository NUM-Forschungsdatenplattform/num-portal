package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.admin.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {

  @ApiModelProperty(value = "The unique identifier", example = "1")
  private Long id;

  @ApiModelProperty
  @NotEmpty(message = "The text of the comment cannot be empty")
  @NotNull(message = "The text of the comment is mandatory")
  private String text;

  @ApiModelProperty private Long projectId;

  @ApiModelProperty private User author;

  private OffsetDateTime createDate;
}
