package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.service.TemplateService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@AllArgsConstructor
@RequestMapping("/template")
public class TemplateController {

  private final TemplateService templateService;

  @GetMapping("/metadata")
  @ApiOperation(value = "Retrieves a list of template metadata")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<List<TemplateMetadataDto>> getAllTemplatesMetadata() {
    return ResponseEntity.ok(templateService.getAllTemplatesMetadata());
  }
}
