package de.vitagroup.num.service;

import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import de.vitagroup.num.web.feign.KeycloakFeign;
import feign.FeignException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

  private final KeycloakFeign keycloakFeign;
  private final UserDetailsService userDetailsService;
  private final OrganizationService organizationService;

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
   * Assigns roles to a particular user
   *
   * @param userId the user to update roles of
   * @param roleNames The list of roles of the user
   */
  public List<String> setUserRoles(String userId, @NotNull List<String> roleNames) {
    try {
      Map<String, Role> supportedRoles =
          keycloakFeign.getRoles().stream().collect(Collectors.toMap(Role::getName, role -> role));

      Set<Role> existingRoles = keycloakFeign.getRolesOfUser(userId);

      Role[] removeRoles =
          existingRoles.stream()
              .filter(role -> !roleNames.contains(role.getName()))
              .collect(Collectors.toList())
              .toArray(new Role[] {});

      Role[] addRoles =
          roleNames.stream()
              .map(supportedRoles::get)
              .peek(
                  role -> {
                    if (role == null) {
                      throw new BadRequestException("Unknown Role");
                    }
                  })
              .collect(Collectors.toList())
              .toArray(new Role[] {});

      if (removeRoles.length > 0) {
        keycloakFeign.removeRoles(userId, removeRoles);
      }

      if (addRoles.length > 0) {
        keycloakFeign.addRoles(userId, addRoles);
      }
      return roleNames;
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
      user.setApproved(userDetails.get().isApproved());

      if (StringUtils.isNotEmpty(userDetails.get().getOrganizationId())) {

        try {
          OrganizationDto organization =
              organizationService.getOrganizationById(userDetails.get().getOrganizationId());

          user.setOrganizationId(organization.getId());
          user.setOrganizationName(organization.getName());

        } catch (ResourceNotFound e) {
          log.error(
              "Invalid organization id {} for user {}",
              userDetails.get().getOrganizationId(),
              user.getId());
        }
      }
    }
  }

  /**
   * Retrieved a list of users that match the search criteria
   *
   * @param approved Indicates that the user has been approved by the admin
   * @param search A string contained in username, first or last name, or email
   * @return
   */
  public Set<User> searchUsers(Boolean approved, String search) {
    Set<User> users = keycloakFeign.searchUsers(search);
    users.stream().forEach(this::addUserDetails);

    if (approved != null) {
      users.removeIf(user -> approved ? user.isNotApproved() : user.isApproved());
    }

    return users;
  }
}
