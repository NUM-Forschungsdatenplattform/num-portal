/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.service.TemplateService;
import de.vitagroup.num.service.logger.AuditLog;
import io.swagger.annotations.ApiOperation;
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
public class TemplateController {

  private final TemplateService templateService;

  @AuditLog
  @GetMapping("/metadata")
  @ApiOperation(value = "Retrieves a list of template metadata")
  public ResponseEntity<List<TemplateMetadataDto>> getAllTemplatesMetadata(
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(templateService.getAllTemplatesMetadata(principal.getSubject()));
  }
}
