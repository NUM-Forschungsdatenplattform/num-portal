package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.admin.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {

  @Schema(description = "The unique identifier", example = "1")
  private Long id;

  @Schema
  @NotEmpty(message = "The text of the comment cannot be empty")
  @NotNull(message = "The text of the comment is mandatory")
  private String text;

  @Schema private Long projectId;

  @Schema private User author;

  private OffsetDateTime createDate;
}
