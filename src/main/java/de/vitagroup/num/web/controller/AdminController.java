package de.vitagroup.num.web.controller;

import com.fasterxml.jackson.databind.node.TextNode;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.service.AdminService;
import io.swagger.annotations.ApiOperation;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {
  private final AdminService adminService;

  @GetMapping("/roles/{role}/users")
  @ApiOperation(value = "Retrieves a list of users that have the given role")
  public ResponseEntity<Set<User>> getUsersByRole(@NotNull @PathVariable String role) {
    return ResponseEntity.ok(adminService.getUsersByRole(role));
  }

  @GetMapping("/users/{userId}")
  @ApiOperation(value = "Retrieves the information about the given user")
  public ResponseEntity<User> getUser(@NotNull @PathVariable String userId) {
    return ResponseEntity.ok(adminService.getUser(userId));
  }

  @GetMapping("/users/{userId}/roles")
  @ApiOperation(value = "Retrieves the roles of the given user")
  public ResponseEntity<Set<Role>> getRolesOfUser(@NotNull @PathVariable String userId) {
    return ResponseEntity.ok(adminService.getRolesOfUser(userId));
  }

  @PostMapping("/users/{userId}/roles")
  @ApiOperation(value = "Adds the given role to the user")
  public ResponseEntity<String> addRole(
      @NotNull @PathVariable String userId, @NotNull @RequestBody TextNode roleName) {
    adminService.setRole(userId, roleName.asText());
    return ResponseEntity.ok(roleName.asText());
  }

  @DeleteMapping("/users/{userId}/roles")
  @ApiOperation(value = "Removes the given role from the user")
  public ResponseEntity<String> removeRole(
      @NotNull @PathVariable String userId, @NotNull @RequestBody TextNode roleName) {
    adminService.removeRole(userId, roleName.asText());
    return ResponseEntity.ok(roleName.asText());
  }
}
