package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.service.TemplateService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.service.logger.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
