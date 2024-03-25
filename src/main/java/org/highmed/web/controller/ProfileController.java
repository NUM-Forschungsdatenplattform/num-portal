package org.highmed.web.controller;

import org.highmed.domain.model.admin.User;
import org.highmed.service.UserService;
import org.highmed.service.exception.CustomizedExceptionHandler;
import org.highmed.service.logger.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping(value = "/profile", produces = "application/json")
@AllArgsConstructor
public class ProfileController extends CustomizedExceptionHandler {

  private final UserService userService;

  @AuditLog(description = "Read current logged in user's profile")
  @GetMapping()
  @Operation(description = "Retrieves the user profile information")
  public ResponseEntity<User> getUserProfile(@AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(userService.getUserProfile(principal.getSubject()));
  }
}
