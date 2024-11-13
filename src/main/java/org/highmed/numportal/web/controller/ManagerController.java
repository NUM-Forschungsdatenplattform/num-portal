package org.highmed.numportal.web.controller;

import org.highmed.numportal.domain.dto.ManagerProjectDto;
import org.highmed.numportal.domain.dto.QueryDto;
import org.highmed.numportal.domain.model.ExportType;
import org.highmed.numportal.service.ManagerService;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.logger.ContextLog;
import org.highmed.numportal.service.util.ExportHeaderUtil;
import org.highmed.numportal.web.config.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/query", produces = "application/json")
@SecurityRequirement(name = "security_auth")
@ConditionalOnProperty(value = "feature.search-with-aql", havingValue = "true")
public class ManagerController {

  private final EhrBaseService ehrBaseService;
  private final ManagerService managerService;
  private final ExportHeaderUtil exportHeaderUtil;

  @ContextLog(type = "Manager", description = "Execute AQL queries")
  @PostMapping("execute")
  @Operation(description = "Executes an AQL query")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<QueryResponseData> executeManagerQuery(
      @RequestBody @Valid QueryDto queryDto) {
    return ResponseEntity.ok(
        ehrBaseService.executePlainQuery(queryDto.getAql())
    );
  }

  @PostMapping("/manager/execute")
  @Operation(
      description = "Executes the manager project aql in the cohort returning medical data matching the templates")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<String> executeManagerProject(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid ManagerProjectDto managerProjectDto) {
    return ResponseEntity.ok(
        managerService.executeManagerProject(
            managerProjectDto.getCohort(),
            managerProjectDto.getTemplates(),
            principal.getSubject()));
  }

  @PostMapping(value = "/manager/export")
  @Operation(description = "Executes the cohort default configuration returns the result as a csv file attachment")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<StreamingResponseBody> exportManagerResults(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid ManagerProjectDto managerProjectDto,
      @RequestParam(required = false)
      @Parameter(description = "A string defining the output format. Valid values are 'csv' and 'json'. Default is csv.")
      ExportType format) {
    StreamingResponseBody streamingResponseBody =
        managerService.getManagerExportResponseBody(
            managerProjectDto.getCohort(), managerProjectDto.getTemplates(), principal.getSubject(),
            format);
    MultiValueMap<String, String> headers = exportHeaderUtil.getExportHeaders(format, 0L);

    return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
  }
}
