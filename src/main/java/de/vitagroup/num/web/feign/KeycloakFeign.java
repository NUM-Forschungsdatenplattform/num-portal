package de.vitagroup.num.web.feign;

import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import java.util.Set;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "keycloak", url = "${userstore.url}")
public interface KeycloakFeign {
  @GetMapping("/users/{userId}")
  User getUser(@PathVariable String userId);

  @GetMapping("/users/{userId}/role-mappings/realm")
  Set<Role> getRolesOfUser(@PathVariable String userId);

  @PostMapping("/users/{userId}/role-mappings/realm")
  void addRole(@PathVariable String userId, @RequestBody Role[] role);

  @GetMapping("/roles/{role}/users")
  Set<User> getUsersByRole(@PathVariable String role);

  @GetMapping("/roles/{role}")
  Role getRole(@PathVariable String role);
}
