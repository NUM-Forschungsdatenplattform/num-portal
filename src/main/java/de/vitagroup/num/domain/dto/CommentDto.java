package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.model.admin.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

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

  private Long projectId;

  private User author;

  private OffsetDateTime createDate;
}
