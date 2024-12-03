package org.highmed.numportal.web.controller;

import org.highmed.numportal.domain.dto.MessageDto;
import org.highmed.numportal.service.MessageService;
import org.highmed.numportal.service.logger.ContextLog;
import org.highmed.numportal.web.config.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/message", produces = "application/json")
@SecurityRequirement(name = "security_auth")
public class MessageController {

  private final MessageService messageService;

  @ContextLog(type = "MessageManagement", description = "Create user message")
  @PostMapping()
  @Operation(
      description = "Creates a message for the users")
  @PreAuthorize(Role.CONTENT_ADMIN)
  public ResponseEntity<MessageDto> createUserMessage(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody MessageDto messageDto) {
    return ResponseEntity.ok(messageService.createUserMessage(messageDto, principal.getSubject()));
  }

  @ContextLog(type = "MessageManagement", description = "Update a message")
  @PutMapping(value = "/{id}")
  @Operation(
      description = "Update a message")
  @PreAuthorize(Role.CONTENT_ADMIN)
  public ResponseEntity<MessageDto> updateUserMessage(
      @PathVariable("id") Long id,
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody MessageDto messageDto) {
    return ResponseEntity.ok(messageService.updateUserMessage(id, messageDto, principal.getSubject()));
  }
}
