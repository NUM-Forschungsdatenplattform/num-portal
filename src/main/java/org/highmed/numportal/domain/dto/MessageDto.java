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

  @NotNull
  Long id;

  private String title;

  @NotNull
  @NotEmpty
  private String text;

  private LocalDateTime startDate;

  private LocalDateTime endDate;

  @NotNull
  private MessageType type;

}
