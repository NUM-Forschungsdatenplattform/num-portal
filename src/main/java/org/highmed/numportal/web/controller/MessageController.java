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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

  @GetMapping()
  @Operation(
      description = "Get list of all pageable user messages sorted default by startdate")
  @PreAuthorize(Role.CONTENT_ADMIN)
  public ResponseEntity<Page<MessageDto>> getUserMessages(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PageableDefault(size = 100, sort = "startDate") Pageable pageable) {
    return ResponseEntity.ok(messageService.getMessages(principal.getSubject(), pageable));
  }

  @ContextLog(type = "MessageManagement", description = "Update a user message")
  @PutMapping(value = "/{id}")
  @Operation(
      description = "Update a user message")
  @PreAuthorize(Role.CONTENT_ADMIN)
  public ResponseEntity<MessageDto> updateUserMessage(
      @PathVariable("id") Long id,
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody MessageDto messageDto) {
    return ResponseEntity.ok(messageService.updateUserMessage(id, messageDto, principal.getSubject()));
  }

  @ContextLog(type = "MessageManagement", description = "Extend End date of a user message")
  @PatchMapping(value = "/{id}")
  @Operation(
      description = "Extend End date of a user message")
  @PreAuthorize(Role.CONTENT_ADMIN)
  public ResponseEntity<MessageDto> deleteActiveUserMessage(
      @PathVariable("id") Long id,
      @AuthenticationPrincipal @NotNull Jwt principal) {
    messageService.deleteActiveUserMessage(id, principal.getSubject());
    return ResponseEntity.ok().build();
  }

  @ContextLog(type = "MessageManagement", description = "Delete a user message")
  @DeleteMapping(value = "/{id}")
  @Operation(
      description = "Delete a user message")
  @PreAuthorize(Role.CONTENT_ADMIN)
  public ResponseEntity<Void> deleteUserMessage(
      @PathVariable("id") Long id,
      @AuthenticationPrincipal @NotNull Jwt principal) {
    messageService.deleteUserMessage(id, principal.getSubject());
    return ResponseEntity.ok().build();
  }

  @PostMapping(value = "/read/{id}")
  @Operation(
      description = "Marked a user message as read by logged in user")
  public ResponseEntity<Void> markUserMessageAsRead(
      @PathVariable("id") Long id,
      @AuthenticationPrincipal @NotNull Jwt principal) {
    messageService.markUserMessageAsRead(id, principal.getSubject());
    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/read")
  @Operation(
      description = "Get a list of all not marked and session based user messages that are currently active")
  public ResponseEntity<List<MessageDto>> getAllDisplayedUserMessages(
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(messageService.getAllDisplayedUserMessages(principal.getSubject()));
  }
}


