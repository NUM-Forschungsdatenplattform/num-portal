package de.vitagroup.num.service;

import de.vitagroup.num.domain.UserDetails;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import de.vitagroup.num.web.feign.KeycloakFeign;
import feign.FeignException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException(
          "An error has occurred, cannot retrieve users, please try again later");
    } catch (FeignException.NotFound e) {
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
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException(
          "An error has occurred, cannot retrieve user roles, please try again later");
    } catch (FeignException.NotFound e) {
      throw new ResourceNotFound("No roles found");
    }
  }

  /**
   * Assigns a role to a particular user
   *
   * @param userId the user to add the role to
   * @param roleName The name of the role to add to the user
   */
  public void setUserRole(String userId, String roleName) {
    try {
      Role role = keycloakFeign.getRole(roleName);

      if (role == null) {
        throw new BadRequestException("Invalid role");
      }

      keycloakFeign.addRole(userId, new Role[] {role});
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException("An error has occurred, please try again later");
    } catch (FeignException.NotFound e) {
      throw new ResourceNotFound("Role or user not found");
    }
  }

  private void addUserDetails(User user) {
    if (user == null) {
      return;
    }

    Optional<UserDetails> userDetails = userDetailsService.getUserDetailsById(user.getId());

    if (userDetails.isPresent()) {
      user.setApproved(userDetails.get().getApproved());
      user.setExternalOrganizationId(userDetails.get().getOrganizationId());
    }
  }

  /**
   * List all users with entry in userdetails table and with requested approved status. Ignores
   * users that have entry in userdetails table but don't exist in keycloak to allow listing users
   * even when there is an invalid entry in the userdetails table.
   *
   * @param approved Either "true" or "false" to get approved or unapproved users.
   * @return List of users with given approval status.
   */
  public List<User> getUsersByApproved(boolean approved) {
    Optional<List<UserDetails>> userDetails = userDetailsService.getApprovedUsers(approved);
    return userDetails
        .map(
            userDetailsSet ->
                userDetailsSet.stream()
                    .map(this::getUserIfExists)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))
        .orElse(new ArrayList<>());
  }

  /**
   * Get user from the user store and add the details info to it.
   *
   * @param userDetails the user details of the user to get
   * @return the user with details, if user is not found, returns null to allow listing users even
   *     with invalid entry in the user details table
   */
  private User getUserIfExists(UserDetails userDetails) {
    try {
      User user = keycloakFeign.getUser(userDetails.getUserId());
      user.setExternalOrganizationId(userDetails.getOrganizationId());
      user.setApproved(userDetails.getApproved());
      return user;
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException("An error has occurred, please try again later");
    } catch (FeignException.NotFound e) {
      log.error("Error while fetching user from keycloak using id from userdetails.", e);
    }
    return null;
  }
}
