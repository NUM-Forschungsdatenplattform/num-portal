package de.vitagroup.num.service;

import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.mapper.OrganizationMapper;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import de.vitagroup.num.web.feign.KeycloakFeign;
import feign.FeignException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

  private final KeycloakFeign keycloakFeign;
  private final UserDetailsService userDetailsService;
  private final OrganizationMapper organizationMapper;

  public User getUserProfile(String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    return getUserById(loggedInUserId, true);
  }

  public User getUserById(String userId, boolean withRole, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    return getUserById(userId, withRole);
  }

  /**
   * Retrieves user, portal user details and corresponding roles from identity provider
   *
   * @param userId External id of the user
   * @return User
   */
  @Transactional
  public User getUserById(String userId, Boolean withRole) {
    try {
      User user = keycloakFeign.getUser(userId);

      if (BooleanUtils.isTrue(withRole)) {
        addRoles(user);
      }

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
  public Set<Role> getUserRoles(String userId, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    return getUserRoles(userId);
  }

  /**
   * Assigns roles to a particular user
   *
   * @param userId the user to update roles of
   * @param roleNames The list of roles of the user
   */
  public List<String> setUserRoles(
      String userId,
      @NotNull List<String> roleNames,
      String callerUserId,
      List<String> callerRoles) {

    UserDetails userToChange =
        userDetailsService
            .getUserDetailsById(userId)
            .orElseThrow(() -> new SystemException("User not found"));

    UserDetails callerDetails = userDetailsService.checkIsUserApproved(callerUserId);

    if (callerRoles.contains(Roles.ORGANIZATION_ADMIN)
        && !callerRoles.contains(Roles.SUPER_ADMIN)
        && !callerDetails
            .getOrganization()
            .getId()
            .equals(userToChange.getOrganization().getId())) {
      throw new ForbiddenException(
          "Organization admin can only manage users in the own organization.");
    }

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
              .filter(
                  role -> existingRoles.stream().noneMatch(role1 -> role1.getName().equals(role)))
              .map(supportedRoles::get)
              .peek(
                  role -> {
                    if (role == null) {
                      throw new BadRequestException("Unknown Role");
                    }
                  })
              .collect(Collectors.toList())
              .toArray(new Role[] {});

      if (Arrays.stream(removeRoles)
          .anyMatch(role -> !Roles.isAllowedToSet(role.getName(), callerRoles))) {
        throw new ForbiddenException("Not allowed to remove that role");
      }
      if (Arrays.stream(addRoles)
          .anyMatch(role -> !Roles.isAllowedToSet(role.getName(), callerRoles))) {
        throw new ForbiddenException("Not allowed to set that role");
      }
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

  private Set<Role> getUserRoles(String userId) {
    try {
      return keycloakFeign.getRolesOfUser(userId);
    } catch (FeignException.BadRequest | FeignException.InternalServerError e) {
      throw new SystemException(
          "An error has occurred, cannot retrieve user roles, please try again later");
    } catch (FeignException.NotFound e) {
      throw new ResourceNotFound("No roles found");
    }
  }

  private void addUserDetails(User user) {
    if (user == null) {
      return;
    }

    Optional<UserDetails> userDetails = userDetailsService.getUserDetailsById(user.getId());

    if (userDetails.isPresent()) {
      user.setApproved(userDetails.get().isApproved());

      if (userDetails.get().getOrganization() != null) {
        user.setOrganization(organizationMapper.convertToDto(userDetails.get().getOrganization()));
      }
    }
  }

  private void addRoles(User user) {
    Set<Role> roles = getUserRoles(user.getId());
    user.setRoles(roles.stream().map(Role::getName).collect(Collectors.toSet()));
  }

  /**
   * Retrieved a list of users that match the search criteria
   *
   * @param approved Indicates that the user has been approved by the admin
   * @param search A string contained in username, first or last name, or email
   * @param withRoles flag whether to add roles to the user structure, if present, or not
   * @return the users that match the search parameters and with optional roles if indicated
   */
  public Set<User> searchUsers(
      String loggedInUserId,
      Boolean approved,
      String search,
      Boolean withRoles,
      List<String> callerRoles) {

    UserDetails loggedInUser = userDetailsService.checkIsUserApproved(loggedInUserId);

    Set<User> users = keycloakFeign.searchUsers(search);
    if (users == null) {
      return Collections.emptySet();
    }
    users.removeIf(u -> userDetailsService.getUserDetailsById(u.getId()).isEmpty());
    users.forEach(this::addUserDetails);

    if ((withRoles != null && withRoles) || callerRoles.contains(Roles.STUDY_COORDINATOR)) {
      users.forEach(this::addRoles);
    }

    if (approved != null) {
      users.removeIf(user -> approved ? user.isNotApproved() : user.isApproved());
    }

    return filterByCallerRole(users, callerRoles, loggedInUser);
  }

  @Transactional
  public Set<User> getByRole(String role) {
    Set<User> users = keycloakFeign.getByRole(role);
    users.removeIf(u -> userDetailsService.getUserDetailsById(u.getId()).isEmpty());
    users.forEach(this::addUserDetails);
    return users;
  }

  private Set<User> filterByCallerRole(
      Set<User> users, List<String> callerRoles, UserDetails loggedInUser) {
    if (callerRoles.contains(Roles.SUPER_ADMIN)) {
      return users;
    }

    Set<User> outputSet = new HashSet<>();

    if (callerRoles.contains(Roles.ORGANIZATION_ADMIN) && loggedInUser.getOrganization() != null) {
      Long loggedInOrgId = loggedInUser.getOrganization().getId();
      users.forEach(
          user -> {
            if (user.getOrganization() != null
                && loggedInOrgId.equals(user.getOrganization().getId())) {
              outputSet.add(user);
            }
          });
    }

    if (callerRoles.contains(Roles.STUDY_COORDINATOR)) {
      users.forEach(
          user -> {
            if (user.getRoles() != null && user.getRoles().contains(Roles.RESEARCHER)) {
              outputSet.add(user);
            }
          });
    }

    return outputSet;
  }
}
