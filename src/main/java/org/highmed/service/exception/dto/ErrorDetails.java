package org.highmed.service.exception.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ErrorDetails {

  private LocalDateTime timestamp = LocalDateTime.now();

  private String message;

  private Map<? extends Object, ? extends Object> details;

  private int messageId;

  private List<String> argumentsList;

  @Builder
  public ErrorDetails(String message, Map<String, String> details, int messageId, List<String> argumentsList) {
    this.messageId = messageId;
    this.argumentsList = argumentsList;
    this.message = message;
    this.details = details;
  }
}
