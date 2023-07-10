package de.vitagroup.num.web.controller;

import ch.qos.logback.classic.Level;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.dto.UserNameDto;
import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.service.UserService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = "/admin/", produces = "application/json")
@AllArgsConstructor
@Tag(description = "admin controller operations", name = "admin-controller")
@SecurityRequirement(name = "security_auth")
public class AdminController extends CustomizedExceptionHandler {

  private static final String SUCCESS_REPLY = "Success";
  private static final String EMAIL_CLAIM = "email";

  private final UserService userService;

  private final UserDetailsService userDetailsService;

  private final HealthEndpoint healthEndpoint;

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
    ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    return ResponseEntity.ok(rootLogger.getLevel());
  }

  @PostMapping("/log-level/{logLevel}")
  public ResponseEntity<Level> setLogLevel(@NotNull @PathVariable String logLevel) {
    ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.valueOf(logLevel));//Default log level is DEBUG. If {logLevel} == Wrong Status
    return ResponseEntity.ok(rootLogger.getLevel());
  }

  @AuditLog
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
  public ResponseEntity<Set<de.vitagroup.num.domain.admin.Role>> getRolesOfUser(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserRoles(userId, principal.getSubject()));
  }

  @AuditLog
  @PostMapping("user/{userId}/role")
  @Operation(description = "Updates the users roles to the given set.")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<List<String>> updateRoles(@AuthenticationPrincipal @NotNull Jwt principal,
                                                  @NotNull @PathVariable String userId, @NotNull @RequestBody List<String> roles) {

    List<String> updatedRoles = userService.setUserRoles(userId, roles, principal.getSubject(), Roles.extractRoles(principal));
    userService.addUserToCache(userId);
    return ResponseEntity.ok(updatedRoles);
  }

  @AuditLog
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

  @AuditLog
  @PostMapping("user/{userId}/name")
  @Operation(description = "Changes user name")
  public ResponseEntity<String> changeUserName(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId,
      @NotNull @Valid @RequestBody UserNameDto userName) {
    userService.changeUserName(userId, userName, principal.getSubject(), Roles.extractRoles(principal));
    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog
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

  @AuditLog
  @PostMapping("user/{userId}/status")
  @Operation(description = "Updates user's status for active flag (enabled field in keycloak representation).")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<String> updateUserDetails(@AuthenticationPrincipal @NotNull Jwt principal,
                                                  @NotNull @PathVariable String userId, @NotNull @RequestBody Boolean active) {
    userService.updateUserActiveField(principal.getSubject(), userId, active, Roles.extractRoles(principal));
    return ResponseEntity.ok(SUCCESS_REPLY);
  }
}
