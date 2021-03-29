/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.web.feign;

import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import java.util.Set;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "keycloak", url = "${userstore.url}")
public interface KeycloakFeign {
  @GetMapping("/users/{userId}")
  User getUser(@PathVariable String userId);

  @GetMapping("/users/{userId}/role-mappings/realm")
  Set<Role> getRolesOfUser(@PathVariable String userId);

  @PostMapping("/users/{userId}/role-mappings/realm")
  void addRoles(@PathVariable String userId, @RequestBody Role[] role);

  @DeleteMapping("/users/{userId}/role-mappings/realm")
  void removeRoles(@PathVariable String userId, @RequestBody Role[] role);

  @GetMapping("/roles")
  Set<Role> getRoles();

  @GetMapping("/users")
  Set<User> searchUsers(@RequestParam(required = false) String search);
}
