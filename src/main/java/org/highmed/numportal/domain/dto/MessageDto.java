package org.highmed.numportal.domain.dto;

import org.highmed.numportal.domain.model.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Schema
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  Long id;

  @NotNull
  @NotEmpty
  private String title;

  private String text;

  @NotNull
  private LocalDateTime startDate;

  @NotNull
  private LocalDateTime endDate;

  @NotNull
  private MessageType type;

  @NotNull
  @Schema(description = "Set this value, so that a user message is readable during each session")
  private boolean sessionBased;
}
