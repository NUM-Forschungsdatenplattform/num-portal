package de.vitagroup.num.service;

import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.web.feign.KeycloakFeign;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AdminService {
  private final KeycloakFeign keycloakFeign;

  public Set<User> getUsersByRole(String role) {
    return keycloakFeign.getUsersByRole(role);
  }

  public User getUser(String userId) {
    User user = keycloakFeign.getUser(userId);
    // Query for roles as they're not returned by user query
    return fetchRoles(user);
  }

  public Set<Role> getRolesOfUser(String userId) {
    return keycloakFeign.getRolesOfUser(userId);
  }

  public void setRole(String userId, String roleName) {
    Role role = keycloakFeign.getRole(roleName);
    keycloakFeign.addRole(userId, new Role[] {role});
  }

  public void removeRole(String userId, String roleName) {
    Role role = keycloakFeign.getRole(roleName);
    keycloakFeign.removeRole(userId, new Role[] {role});
  }

  private User fetchRoles(User user) {
    Set<Role> roles = keycloakFeign.getRolesOfUser(user.getId());
    user.setRoles(roles.stream().map(Role::getName).collect(Collectors.toSet()));
    return user;
  }
}
