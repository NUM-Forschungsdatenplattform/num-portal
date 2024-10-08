package org.highmed.numportal.domain.dto;

import org.highmed.numportal.domain.model.admin.User;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
