package de.vitagroup.num.web.feign;

import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;

@FeignClient(name = "keycloak", url = "${feign.client.config.numPortal.url}")
public interface KeycloakFeign {
    @GetMapping("/users/{userId}")
    User getUSer(@PathVariable String userId);

    @GetMapping("/users/{userId}/role-mappings/realm")
    Set<Role> getRolesOfUser(@PathVariable String userId);

    @GetMapping("/roles/{role}/users")
    Set<User> getUsersByRole(@PathVariable String role);
}
