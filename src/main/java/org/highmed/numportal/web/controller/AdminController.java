package org.highmed.numportal.web.controller;

import org.highmed.numportal.NumPortalApplication;
import org.highmed.numportal.domain.dto.OrganizationDto;
import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.dto.UserNameDto;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.properties.NumProperties;
import org.highmed.numportal.service.UserDetailsService;
import org.highmed.numportal.service.UserService;
import org.highmed.numportal.service.ehrbase.Pseudonymity;
import org.highmed.numportal.service.exception.CustomizedExceptionHandler;
import org.highmed.numportal.service.logger.ContextLog;
import org.highmed.numportal.web.config.Role;
import org.highmed.numportal.web.feign.KeycloakFeign;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = "/admin/", produces = "application/json")
@AllArgsConstructor
@Tag(description = "admin controller operations", name = "admin-controller")
@SecurityRequirement(name = "security_auth")
public class AdminController extends CustomizedExceptionHandler {

  private static final String SUCCESS_REPLY = "Success";
  private static final String EMAIL_CLAIM = "email";
  private static final String USER_MANAGEMENT = "UserManagement";

  private final UserService userService;

  private final UserDetailsService userDetailsService;

  private final Pseudonymity pseudonymity;

  private final NumProperties numProperties;


  @GetMapping(value = "manuel-url", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(description = "Returns value for user manual URL")
  public ResponseEntity<Map<String, Object>> getExternalUrls() {
    Map<String, Object> map = new HashMap<>();
    map.put("userManualUrl", numProperties.getUserManualUrl());
    return ResponseEntity.ok(map);
  }

  @GetMapping("/log-level")
  @Operation(description = "Returns the set log level")
  public ResponseEntity<Level> getLogLevel() {
    Logger numLogger = (Logger) LoggerFactory.getLogger(NumPortalApplication.class.getPackageName());
    return ResponseEntity.ok(numLogger.getLevel());
  }

  @PostMapping("/log-level/{logLevel}")
  @Operation(description = "Sets the log level for the backend")
  public ResponseEntity<Level> setLogLevel(@NotNull @PathVariable String logLevel) {
    Logger numLogger = (Logger) LoggerFactory.getLogger(NumPortalApplication.class.getPackageName());
    Level level = Level.valueOf(logLevel);
    numLogger.setLevel(level); //Default log level is DEBUG. If {logLevel} == Wrong Status
    if (Level.DEBUG.equals(level) || Level.INFO.equals(level)) {
      // when DEBUG, logging is enabled for keycloak client
      Logger keycloakClient = (Logger) LoggerFactory.getLogger(KeycloakFeign.class.getName());
      keycloakClient.setLevel(numLogger.getLevel());
    }
    return ResponseEntity.ok(numLogger.getLevel());
  }

  @ContextLog(type = USER_MANAGEMENT, description = "Delete User")
  @DeleteMapping("user/{userId}")
  @PreAuthorize(Role.SUPER_ADMIN)
  @Operation(description = "Deletes the user with given id")
  public void deleteUser(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable String userId) {
    userService.deleteUser(userId, principal.getSubject());
  }

  @GetMapping("user/{userId}")
  @Operation(description = "Retrieves the information about the given user")
  public ResponseEntity<User> getUser(@AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserById(userId, true, principal.getSubject()));
  }

  @GetMapping("user/{userId}/role")
  @Operation(description = "Retrieves the roles of the given user")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<Set<org.highmed.numportal.domain.model.admin.Role>> getRolesOfUser(@AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserRoles(userId, principal.getSubject()));
  }

  @ContextLog(type = "UserManagement", description = "Update user roles")
  @PostMapping("user/{userId}/role")
  @Operation(description = "Updates the users roles to the given set.")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<List<String>> updateRoles(@AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId,
      @NotNull @RequestBody List<String> roles) {

    List<String> updatedRoles = userService.setUserRoles(userId, roles, principal.getSubject(), Roles.extractRoles(principal));
    userService.addUserToCache(userId);
    return ResponseEntity.ok(updatedRoles);
  }

  @ContextLog(type = "UserManagement", description = "Update user organization")
  @PostMapping("user/{userId}/organization")
  @Operation(description = "Sets the user organization")
  @PreAuthorize(Role.SUPER_ADMIN)
  public ResponseEntity<String> setOrganization(@AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId,
      @NotNull @RequestBody OrganizationDto organization) {

    userDetailsService.setOrganization(principal.getSubject(), userId, organization.getId());
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @ContextLog(type = "UserManagement", description = "Creates user details")
  @PostMapping("user/{userId}")
  @Operation(description = "Creates user details")
  public ResponseEntity<String> createUserOnFirstLogin(@AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {

    userDetailsService.createUserDetails(userId, principal.getClaimAsString(EMAIL_CLAIM));
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @ContextLog(type = "UserManagement", description = "Update user name", dtoPrint = false)
  @PostMapping("user/{userId}/name")
  @Operation(description = "Changes user name")
  public ResponseEntity<String> changeUserName(@AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId,
      @NotNull @Valid @RequestBody UserNameDto userName) {
    userService.changeUserName(userId, userName, principal.getSubject(), Roles.extractRoles(principal));
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @ContextLog(type = "UserManagement", description = "Approve user")
  @PostMapping("user/{userId}/approve")
  @Operation(description = "Adds the given organization to the user")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<String> approveUser(@AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    userDetailsService.approveUser(principal.getSubject(), userId);
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @GetMapping("user/all")
  @Operation(description = "Retrieves a set of users that match the search string")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN_OR_STUDY_COORDINATOR)
  public ResponseEntity<Page<User>> searchUsersWithPagination(@AuthenticationPrincipal @NotNull Jwt principal,
      @PageableDefault(size = 100) Pageable pageable, SearchCriteria criteria) {
    // filter[approved] true, false (optional -> omitting it returns both)
    // filter[search] search input (optional)
    // filter[withRoles] true or false (optional)
    // filter[enabled] true or false (optional)
    return ResponseEntity.ok(userService.searchUsers(principal.getSubject(), Roles.extractRoles(principal), criteria, pageable));
  }


  @ContextLog(type = "UserManagement", description = "Update user active field")
  @PostMapping("user/{userId}/status")
  @Operation(description = "Updates user status for active flag (enabled field in keycloak representation).")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<String> updateUserDetails(@AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId,
      @NotNull @RequestBody Boolean active) {
    userService.updateUserActiveField(principal.getSubject(), userId, active, Roles.extractRoles(principal));
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

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
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + String.format("codex_result_%s_%s",
        LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE), csvFile.getOriginalFilename()));
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
