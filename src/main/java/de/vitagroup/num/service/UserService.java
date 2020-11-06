package de.vitagroup.num.service;

import de.vitagroup.num.domain.UserDetails;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import de.vitagroup.num.web.feign.KeycloakFeign;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.vitagroup.num.web.feign.exception.FeignBadRequestException;
import de.vitagroup.num.web.feign.exception.FeignResourceNotFoundException;
import de.vitagroup.num.web.feign.exception.FeignSystemException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

  private final KeycloakFeign keycloakFeign;
  private final UserDetailsService userDetailsService;

  /**
   * Retrieves a set of users from the identity provider
   *
   * @param role Role of the user
   * @return Set of users
   */
  public Set<User> getUsersByRole(String role) {
    try {

      Set<User> users = keycloakFeign.getUsersByRole(role);
      users.forEach(this::addUserDetails);
      return users;

    } catch (FeignBadRequestException | FeignSystemException e) {
      throw new SystemException(
          "An error has occurred, cannot retrieve user, please try again later");
    } catch (FeignResourceNotFoundException e) {
      throw new ResourceNotFound("No users found");
    }
  }

  /**
   * Retrieves user, portal user details and corresponding roles from identity provider
   *
   * @param userId External id of the user
   * @return User
   */
  public User getUserById(String userId) {
    try {

      User user = keycloakFeign.getUser(userId);
      Set<Role> roles = getUserRoles(user.getId());
      user.setRoles(roles.stream().map(Role::getName).collect(Collectors.toSet()));
      addUserDetails(user);
      return user;

    } catch (FeignBadRequestException | FeignSystemException e) {
      throw new SystemException(
          "An error has occurred, cannot retrieve users, please try again later");
    } catch (FeignResourceNotFoundException e) {
      throw new ResourceNotFound("User not found");
    }
  }

  /**
   * Retrieves a set of user roles for a particular user from identity provider
   *
   * @param userId External id of the user
   * @return Set of roles
   */
  public Set<Role> getUserRoles(String userId) {
    try {
      return keycloakFeign.getRolesOfUser(userId);
    } catch (FeignBadRequestException | FeignSystemException e) {
      throw new SystemException(
          "An error has occurred, cannot retrieve user roles, please try again later");
    } catch (FeignResourceNotFoundException e) {
      throw new ResourceNotFound("No roles found");
    }
  }

  /**
   * Assigns a role to a particular user
   *
   * @param userId
   * @param roleName
   */
  public void setUserRole(String userId, String roleName) {
    try {
      Role role = keycloakFeign.getRole(roleName);

      if (role == null) {
        throw new BadRequestException("Invalid role");
      }

      keycloakFeign.addRole(userId, new Role[] {role});
    } catch (FeignBadRequestException | FeignSystemException e) {
      throw new SystemException("An error has occurred, please try again later");
    } catch (FeignResourceNotFoundException e) {
      throw new ResourceNotFound("Role or user not found");
    }
  }

  private void addUserDetails(User user) {
    if (user == null) {
      return;
    }

    Optional<UserDetails> userDetails =
        userDetailsService.getUserDetailsById(user.getId());

    if (userDetails.isPresent()) {
      user.setApproved(userDetails.get().getApproved());
      user.setExternalOrganizationId(userDetails.get().getOrganizationId());
    }
  }
}
