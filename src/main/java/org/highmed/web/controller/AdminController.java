package org.highmed.web.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.highmed.NumPortalApplication;
import org.highmed.domain.model.Roles;
import org.highmed.domain.model.SetupType;
import org.highmed.domain.model.admin.User;
import org.highmed.domain.dto.OrganizationDto;
import org.highmed.domain.dto.SearchCriteria;
import org.highmed.domain.dto.UserNameDto;
import org.highmed.properties.NumProperties;
import org.highmed.service.SetupHealthiness;
import org.highmed.service.UserDetailsService;
import org.highmed.service.UserService;
import org.highmed.service.ehrbase.Pseudonymity;
import org.highmed.service.exception.CustomizedExceptionHandler;
import org.highmed.service.logger.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.highmed.web.config.Role;
import org.highmed.web.feign.KeycloakFeign;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping(value = "/admin/", produces = "application/json")
@AllArgsConstructor
@Tag(description = "admin controller operations", name = "admin-controller")
@SecurityRequirement(name = "security_auth")
public class AdminController extends CustomizedExceptionHandler {

  private static final String SUCCESS_REPLY = "Success";
  private static final String EMAIL_CLAIM = "email";
  private static final String CHECK_FOR_ANNOUNCEMENTS = "CHECK_FOR_ANNOUNCEMENTS";

  private final UserService userService;

  private final UserDetailsService userDetailsService;

  private final HealthEndpoint healthEndpoint;

  private final Pseudonymity pseudonymity;

  private final NumProperties numProperties;

  private final SetupHealthiness healthiness;

  @GetMapping(value = "external-urls", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(description = "Returns value for health status endpoint URL and user manual URL")
  public ResponseEntity<Map<String, Object>> getExternalUrls(){
    Map<String, Object> map = new HashMap<>();
    map.put("systemStatusUrl", numProperties.getSystemStatusUrl());
    map.put("userManualUrl", numProperties.getUserManualUrl());
    return ResponseEntity.ok(map);
  }

  @GetMapping(value = "services-status", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(description = "Returns value for healths of different microservices")
  public ResponseEntity<Map<String, String>> getServicesStatus(
          final @RequestParam(value = "setup", defaultValue = "PREPROD") SetupType setup){
    Map<String, String> map = healthiness.checkHealth(setup);
    if(map.values().stream().filter(s -> s.length() > 0).findFirst().isEmpty()) {
      map.put(CHECK_FOR_ANNOUNCEMENTS, healthiness.checkForAnnouncements());
      return ResponseEntity.ok(map);
    } else {
      map.put(CHECK_FOR_ANNOUNCEMENTS, healthiness.checkForAnnouncements());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
    }
  }

  @GetMapping("health")
  public ResponseEntity<Status> health() {
    if (healthEndpoint.health().getStatus() == Status.UP) {
      return ResponseEntity.ok(healthEndpoint.health().getStatus());
    } else {
      return ResponseEntity.badRequest().body(healthEndpoint.health().getStatus());
    }
  }

  @GetMapping("/log-level")
  public ResponseEntity<Level> getLogLevel() {
    Logger numLogger = (Logger) LoggerFactory.getLogger(NumPortalApplication.class.getPackageName());
    return ResponseEntity.ok(numLogger.getLevel());
  }

  @PostMapping("/log-level/{logLevel}")
  public ResponseEntity<Level> setLogLevel(@NotNull @PathVariable String logLevel) {
    Logger numLogger = (Logger) LoggerFactory.getLogger(NumPortalApplication.class.getPackageName());
    Level level = Level.valueOf(logLevel);
    numLogger.setLevel(level);//Default log level is DEBUG. If {logLevel} == Wrong Status
    if (Level.DEBUG.equals(level) || Level.INFO.equals(level)) {
      // when DEBUG, logging is enabled for keycloak client
      Logger keycloakClient = (Logger) LoggerFactory.getLogger(KeycloakFeign.class.getName());
      keycloakClient.setLevel(numLogger.getLevel());
    }
    return ResponseEntity.ok(numLogger.getLevel());
  }

  @AuditLog(description = "Delete user")
  @DeleteMapping("user/{userId}")
  @PreAuthorize(Role.SUPER_ADMIN)
  public void deleteUser(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable String userId) {
    userService.deleteUser(userId, principal.getSubject());
  }

  @AuditLog
  @GetMapping("user/{userId}")
  @Operation(description = "Retrieves the information about the given user")
  public ResponseEntity<User> getUser(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserById(userId, true, principal.getSubject()));
  }

  @AuditLog
  @GetMapping("user/{userId}/role")
  @Operation(description = "Retrieves the roles of the given user")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<Set<org.highmed.domain.model.admin.Role>> getRolesOfUser(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserRoles(userId, principal.getSubject()));
  }

  @AuditLog(description = "Update user's roles")
  @PostMapping("user/{userId}/role")
  @Operation(description = "Updates the users roles to the given set.")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<List<String>> updateRoles(@AuthenticationPrincipal @NotNull Jwt principal,
                                                  @NotNull @PathVariable String userId, @NotNull @RequestBody List<String> roles) {

    List<String> updatedRoles = userService.setUserRoles(userId, roles, principal.getSubject(), Roles.extractRoles(principal));
    userService.addUserToCache(userId);
    return ResponseEntity.ok(updatedRoles);
  }

  @AuditLog(description = "Update user's organization")
  @PostMapping("user/{userId}/organization")
  @Operation(description = "Sets the user's organization")
  @PreAuthorize(Role.SUPER_ADMIN)
  public ResponseEntity<String> setOrganization(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable String userId,
      @NotNull @RequestBody OrganizationDto organization) {

    userDetailsService.setOrganization(principal.getSubject(), userId, organization.getId());
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog
  @PostMapping("user/{userId}")
  @Operation(description = "Creates user details")
  public ResponseEntity<String> createUserOnFirstLogin(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {

    userDetailsService.createUserDetails(userId, principal.getClaimAsString(EMAIL_CLAIM));
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog(description = "Update user's name")
  @PostMapping("user/{userId}/name")
  @Operation(description = "Changes user name")
  public ResponseEntity<String> changeUserName(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId,
      @NotNull @Valid @RequestBody UserNameDto userName) {
    userService.changeUserName(userId, userName, principal.getSubject(), Roles.extractRoles(principal));
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog(description = "Approve user")
  @PostMapping("user/{userId}/approve")
  @Operation(description = "Adds the given organization to the user")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<String> approveUser(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    userDetailsService.approveUser(principal.getSubject(), userId);
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog
  @GetMapping("user/all")
  @Operation(description = "Retrieves a set of users that match the search string")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN_OR_STUDY_COORDINATOR)
  public ResponseEntity<Page<User>> searchUsersWithPagination(@AuthenticationPrincipal @NotNull Jwt principal, @PageableDefault(size = 100) Pageable pageable,
                                                              SearchCriteria criteria) {
    // filter[approved] true, false (optional -> omitting it returns both)
    // filter[search] search input (optional)
    // filter[withRoles] true or false (optional)
    // filter[enabled] true or false (optional)
    return ResponseEntity.ok(
            userService.searchUsers(principal.getSubject(), Roles.extractRoles(principal), criteria, pageable));
  }

  @AuditLog(description = "Update user's active field")
  @PostMapping("user/{userId}/status")
  @Operation(description = "Updates user's status for active flag (enabled field in keycloak representation).")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<String> updateUserDetails(@AuthenticationPrincipal @NotNull Jwt principal,
                                                  @NotNull @PathVariable String userId, @NotNull @RequestBody Boolean active) {
    userService.updateUserActiveField(principal.getSubject(), userId, active, Roles.extractRoles(principal));
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog
  @PostMapping(path = "pseudo/test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(description = "Endpoint used for testing 3rd party pseudonyms")
  @PreAuthorize(Role.SUPER_ADMIN)
  public ResponseEntity<StreamingResponseBody> testThirdPartyPseudonyms(@AuthenticationPrincipal @NotNull Jwt principal,
                                                                        @NotNull @RequestParam("file") MultipartFile csvFile) throws IOException {
    List<String> secondLevelPseudonyms = new ArrayList<>();
    String header = "original,pseudonym";
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream()))) {
      String code;
      while ((code = reader.readLine()) != null) {
        secondLevelPseudonyms.add(code);
      }
    }
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" +
                    String.format("codex_result_%s_%s",
                            LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE),
                            csvFile.getOriginalFilename()));
    StreamingResponseBody streamingResponseBody = outputStream -> {
      BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
      bufferedWriter.write(header);
      bufferedWriter.newLine();
      for (String code : secondLevelPseudonyms) {
        List<String> pseudonymsResponse = pseudonymity.getPseudonyms(List.of(code), 1L);
        String currentLine = code + ',' + pseudonymsResponse.get(0);
        bufferedWriter.write(currentLine);
        bufferedWriter.newLine();
      }
      bufferedWriter.flush();
      bufferedWriter.close();
    };
    return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
  }
}
