package de.vitagroup.num.web.controller;

import com.fasterxml.jackson.databind.node.TextNode;
import de.vitagroup.num.converter.UserDetailsConverter;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {

  private final UserService userService;
  private final UserDetailsService userDetailsService;
  private final UserDetailsConverter converter;

  @GetMapping("/user/{userId}")
  @ApiOperation(value = "Retrieves the information about the given user")
  public ResponseEntity<User> getUser(@NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserById(userId));
  }

  @GetMapping("/user/{userId}/role")
  @ApiOperation(value = "Retrieves the roles of the given user")
  public ResponseEntity<Set<Role>> getRolesOfUser(@NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userService.getUserRoles(userId));
  }

  @PostMapping("/user/{userId}/role")
  @ApiOperation(value = "Adds the given role to the user")
  public ResponseEntity<String> addRole(
      @NotNull @PathVariable String userId, @NotNull @RequestBody TextNode roleName) {
    userService.setUserRole(userId, roleName.asText());
    return ResponseEntity.ok(roleName.asText());
  }

  @PostMapping("/user/{userId}/organization")
  @ApiOperation(value = "Adds the given organization to the user")
  public ResponseEntity<String> addOrganization(
      @NotNull @PathVariable String userId, @NotNull @RequestBody OrganizationDto organization) {
    userDetailsService.createOrUpdateUserDetails(userId, organization.getId());
    return ResponseEntity.ok("Success");
  }

  @PostMapping("/user/details")
  @ApiOperation(value = "Creates user details")
  public ResponseEntity<UserDetailsDto> addOrganization(
      @NotNull @Valid @RequestBody UserDetailsDto userDetailsDto) {
    UserDetails userDetails =
        userDetailsService.createUserDetails(converter.convertToEntity(userDetailsDto));
    return ResponseEntity.ok(converter.convertToDto(userDetails));
  }

  @PostMapping("/user/{userId}/approve")
  @ApiOperation(value = "Adds the given organization to the user")
  public ResponseEntity<UserDetails> approveUSer(@NotNull @PathVariable String userId) {
    return ResponseEntity.ok(userDetailsService.approveUser(userId));
  }

  @GetMapping("/user")
  @ApiOperation(value = "Retrieves the users that match the query parameters")
  public ResponseEntity<List<User>> getUsersByApproved(
      @ApiParam(value = "Switch to show only approved or unapproved users.", required = true)
          @RequestParam
          boolean approved) {
    return ResponseEntity.ok(userService.getUsersByApproved(approved));
  }
}
