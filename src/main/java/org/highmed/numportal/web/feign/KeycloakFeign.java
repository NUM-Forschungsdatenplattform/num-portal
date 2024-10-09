package org.highmed.numportal.web.feign;

import org.highmed.numportal.domain.model.admin.Role;
import org.highmed.numportal.domain.model.admin.User;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

@FeignClient(name = "keycloak", url = "${userstore.url}")
public interface KeycloakFeign {

  @GetMapping("/users/{userId}")
  User getUser(@PathVariable String userId);

  @DeleteMapping("/users/{userId}")
  User deleteUser(@PathVariable String userId);

  @GetMapping("/users/{userId}/role-mappings/realm")
  Set<Role> getRolesOfUser(@PathVariable String userId);

  @GetMapping("/users/count")
  Long countUsers(@RequestParam("enabled") boolean enabled);

  @PostMapping("/users/{userId}/role-mappings/realm")
  void addRoles(@PathVariable String userId, @RequestBody Role[] role);

  @DeleteMapping("/users/{userId}/role-mappings/realm")
  void removeRoles(@PathVariable String userId, @RequestBody Role[] role);

  @GetMapping("/roles")
  Set<Role> getRoles();

  @GetMapping("/roles/{roleName}/users")
  Set<User> getByRole(@PathVariable String roleName);

  @GetMapping("/users/{userId}")
  Map<String, Object> getUserRaw(@PathVariable String userId);

  @PutMapping("/users/{userId}")
  void updateUser(@PathVariable String userId, @RequestBody Map<String, Object> userMap);
}
