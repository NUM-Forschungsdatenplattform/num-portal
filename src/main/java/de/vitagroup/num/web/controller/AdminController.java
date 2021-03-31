package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.service.UserService;
import de.vitagroup.num.service.email.Notification;
import de.vitagroup.num.service.email.NotificationService;
import de.vitagroup.num.service.email.NotificationType;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/admin", produces = "application/json")
@AllArgsConstructor
public class AdminController {

  private static final String SUCCESS_REPLY = "Success";
  private static final String EMAIL_CLAIM = "email";

  private final UserService userService;

  private final UserDetailsService userDetailsService;

  private final NotificationService notificationService;

  @AuditLog
  @GetMapping("/user/{userId}")
  @ApiOperation(value = "Retrieves the information about the given user")
  public ResponseEntity<User> getUser(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserById(userId, true, principal.getSubject()));
  }

  @AuditLog
  @GetMapping("/user/{userId}/role")
  @ApiOperation(value = "Retrieves the roles of the given user")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<Set<de.vitagroup.num.domain.admin.Role>> getRolesOfUser(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserRoles(userId, principal.getSubject()));
  }

  @AuditLog
  @PostMapping("/user/{userId}/role")
  @ApiOperation(value = "Updates the users roles to the given set.")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<List<String>> updateRoles(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable String userId,
      @NotNull @RequestBody List<String> roles) {
    List<String> assignedRoles =
        userService.setUserRoles(
            userId, roles, principal.getSubject(), Roles.extractRoles(principal));

    User user = userService.getUserById(principal.getSubject(), false);
    notificationService.notify(
        Notification.builder().type(NotificationType.USER_UPDATE).userId(user.getId()).build());

    return ResponseEntity.ok(assignedRoles);
  }

  @AuditLog
  @PostMapping("/user/{userId}/organization")
  @ApiOperation(value = "Sets the user's organization")
  @PreAuthorize(Role.SUPER_ADMIN)
  public ResponseEntity<String> setOrganization(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable String userId,
      @NotNull @RequestBody OrganizationDto organization) {
    UserDetails user =
        userDetailsService.setOrganization(principal.getSubject(), userId, organization.getId());

    if (user.getOrganization() != null) {
      notificationService.notify(
          Notification.builder().type(NotificationType.NEW_USER).userId(user.getUserId()).build());
    }

    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog
  @PostMapping("/user/{userId}")
  @ApiOperation(value = "Creates user details")
  public ResponseEntity<String> createUserOnFirstLogin(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    UserDetails user =
        userDetailsService.createUserDetails(userId, principal.getClaimAsString(EMAIL_CLAIM));

    if (user.getOrganization() != null) {
      notificationService.notify(
          Notification.builder().type(NotificationType.NEW_USER).userId(user.getUserId()).build());
    } else {
      notificationService.notify(
          Notification.builder()
              .type(NotificationType.NEW_USER_WITHOUT_ORGANIZATION)
              .userId(user.getUserId())
              .build());
    }

    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog
  @PostMapping("/user/{userId}/approve")
  @ApiOperation(value = "Adds the given organization to the user")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<String> approveUser(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable String userId) {
    UserDetails user = userDetailsService.approveUser(principal.getSubject(), userId);

    notificationService.notify(
        Notification.builder().type(NotificationType.USER_UPDATE).userId(user.getUserId()).build());

    return ResponseEntity.ok(SUCCESS_REPLY);
  }

  @AuditLog
  @GetMapping("/user")
  @ApiOperation(value = "Retrieves a set of users that match the search string")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN_OR_STUDY_COORDINATOR)
  public ResponseEntity<Set<User>> searchUsers(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestParam(required = false)
          @ApiParam(
              value =
                  "A flag for controlling whether to list approved or not approved users, omitting it returns both)")
          Boolean approved,
      @RequestParam(required = false)
          @ApiParam(value = "A string contained in username, first or last name, or email")
          String search,
      @RequestParam(required = false)
          @ApiParam(
              value =
                  "A flag for controlling whether to include user's roles in the response (a bit slower)")
          Boolean withRoles) {
    return ResponseEntity.ok(
        userService.searchUsers(
            principal.getSubject(), approved, search, withRoles, Roles.extractRoles(principal)));
  }
}
