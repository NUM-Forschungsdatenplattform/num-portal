package org.highmed.web.controller;

import org.highmed.domain.dto.TemplateMetadataDto;
import org.highmed.service.TemplateService;
import org.highmed.service.exception.CustomizedExceptionHandler;
import org.highmed.service.logger.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping(value = "/template", produces = "application/json")
public class TemplateController extends CustomizedExceptionHandler {

  private final TemplateService templateService;

  @AuditLog
  @GetMapping("/metadata")
  @Operation(description = "Retrieves a list of template metadata")
  public ResponseEntity<List<TemplateMetadataDto>> getAllTemplatesMetadata(
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(templateService.getAllTemplatesMetadata(principal.getSubject()));
  }
}
