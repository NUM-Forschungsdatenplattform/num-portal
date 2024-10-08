package org.highmed.numportal.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.highmed.numportal.domain.dto.QueryDto;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.logger.AuditLog;
import org.highmed.numportal.web.config.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/query", produces = "application/json")
@SecurityRequirement(name = "security_auth")
public class QueryController {

    private final EhrBaseService ehrBaseService;

    @AuditLog
    @PostMapping("execute")
    @Operation(description = "Executes an AQL query")
    @PreAuthorize(Role.MANAGER)
    public ResponseEntity<QueryResponseData> executeManagerProject(
            @RequestBody @Valid QueryDto queryDto) {
        return ResponseEntity.ok(
                ehrBaseService.executePlainQuery(queryDto.getAql()
                ));
    }
}
