package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Organization;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

  @Mock private KeycloakFeign keycloakFeign;

  @Mock private UserDetailsService userDetailsService;

  @Mock private OrganizationMapper organizationMapper;

  @InjectMocks private UserService userService;

  private final Set<Role> roles =
      Set.of(
          new Role("R1", "SUPER_ADMIN"),
          new Role("R2", "RESEARCHER"),
          new Role("R3", "ORGANIZATION_ADMIN"),
          new Role("R4", "STUDY_COORDINATOR"),
          new Role("R5", "CONTENT_ADMIN"),
          new Role("R6", "STUDY_APPROVER"));

  @Before
  public void setup() {
    when(keycloakFeign.getUser("1")).thenThrow(FeignException.BadRequest.class);
    when(keycloakFeign.getUser("2")).thenThrow(FeignException.InternalServerError.class);
    when(keycloakFeign.getUser("3")).thenThrow(FeignException.NotFound.class);

    when(keycloakFeign.getRolesOfUser("1")).thenThrow(FeignException.BadRequest.class);
    when(keycloakFeign.getRolesOfUser("2")).thenThrow(FeignException.InternalServerError.class);
    when(keycloakFeign.getRolesOfUser("3")).thenThrow(FeignException.NotFound.class);
    when(keycloakFeign.getRolesOfUser("4")).thenReturn(Set.of(new Role("R2", "RESEARCHER")));
    when(keycloakFeign.getRolesOfUser("5")).thenReturn(Collections.emptySet());
    when(keycloakFeign.getRolesOfUser("6")).thenReturn(roles);

    when(keycloakFeign.getRoles()).thenReturn(roles);

    when(userDetailsService.getUserDetailsById("4"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("4")
                    .organization(Organization.builder().id(1L).build())
                    .approved(true)
                    .build()));
    when(userDetailsService.validateAndReturnUserDetails("4"))
        .thenReturn(
            UserDetails.builder()
                .userId("4")
                .organization(Organization.builder().id(1L).build())
                .approved(true)
                .build());
    when(userDetailsService.getUserDetailsById("5"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("5")
                    .organization(Organization.builder().id(1L).build())
                    .approved(true)
                    .build()));
    when(userDetailsService.getUserDetailsById("6"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("6")
                    .organization(Organization.builder().id(1L).build())
                    .approved(true)
                    .build()));
    when(userDetailsService.getUserDetailsById("7"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("7")
                    .organization(Organization.builder().id(2L).build())
                    .approved(true)
                    .build()));
  }

  @Test(expected = SystemException.class)
  public void shouldHandleGetUserBadRequest() {
    userService.getUserById("1", true);
  }

  @Test(expected = SystemException.class)
  public void shouldHandleGetUserError() {
    userService.getUserById("2", true);
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleUserNotFound() {
    userService.getUserById("3", true);
  }

  @Test(expected = SystemException.class)
  public void shouldHandleRolesBadRequest() {
    userService.getUserRoles("1");
  }

  @Test(expected = SystemException.class)
  public void shouldHandleRolesError() {
    userService.getUserRoles("2");
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleRolesNotFound() {
    userService.getUserRoles("3");
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleSetInvalidRole() {
    userService.setUserRoles(
        "4",
        Collections.singletonList("non-existent role"),
        "4",
        Collections.singletonList(Roles.SUPER_ADMIN));
  }

  @Test
  public void shouldAddNewRole() {
    userService.setUserRoles(
        "4",
        Collections.singletonList("SUPER_ADMIN"),
        "4",
        Collections.singletonList(Roles.SUPER_ADMIN));
    verify(keycloakFeign, times(1)).removeRoles("4", new Role[] {new Role("R2", "RESEARCHER")});
    verify(keycloakFeign, times(1)).addRoles("4", new Role[] {new Role("R1", "SUPER_ADMIN")});
  }

  @Test
  public void shouldNotSetExistingRole() {
    userService.setUserRoles(
        "4",
        Collections.singletonList("RESEARCHER"),
        "4",
        Collections.singletonList(Roles.SUPER_ADMIN));
    verify(keycloakFeign, never()).removeRoles(anyString(), any(Role[].class));
    verify(keycloakFeign, never()).addRoles("4", new Role[] {new Role("R2", "RESEARCHER")});
  }

  @Test
  public void shouldUnsetRoles() {
    userService.setUserRoles(
        "4", Collections.emptyList(), "4", Collections.singletonList(Roles.SUPER_ADMIN));
    verify(keycloakFeign, times(1)).removeRoles("4", new Role[] {new Role("R2", "RESEARCHER")});
    verify(keycloakFeign, never()).addRoles(anyString(), any(Role[].class));
  }

  @Test
  public void shouldReturnUserWithTimestamp() {
    User user = new User();
    user.setCreatedTimestamp(6234234234L);
    user.setId("4");
    when(keycloakFeign.getUser("4")).thenReturn(user);
    de.vitagroup.num.domain.admin.User userReturn = userService.getUserById("4", true);
    assertThat(userReturn.getCreatedTimestamp(), is(6234234234L));
    verify(keycloakFeign, times(1)).getRolesOfUser("4");
    verify(keycloakFeign, never()).addRoles(anyString(), any(Role[].class));
  }

  @Test
  public void shouldReturnUserWithRoles() {
    User user = new User();
    user.setFirstName("john");
    user.setId("4");
    when(keycloakFeign.searchUsers(null)).thenReturn(Set.of(user));
    Set<de.vitagroup.num.domain.admin.User> userReturn = userService.searchUsers(null, null, true);
    assertThat(userReturn.iterator().next().getRoles().iterator().next(), is("RESEARCHER"));
    verify(keycloakFeign, times(1)).getRolesOfUser("4");
  }

  @Test
  public void shouldReturnUserWithoutRoles() {
    User user = new User();
    user.setFirstName("john");
    user.setId("4");
    when(keycloakFeign.searchUsers(null)).thenReturn(Set.of(user));
    Set<de.vitagroup.num.domain.admin.User> userReturn = userService.searchUsers(null, null, false);
    assertNull(userReturn.iterator().next().getRoles());
    verify(keycloakFeign, times(0)).getRolesOfUser("4");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldShouldNotAllowOrgAdminSetRolesUserDifferentOrg() {
    userService.setUserRoles(
        "7",
        Collections.singletonList("RESEARCHER"),
        "4",
        Collections.singletonList("ORGANIZATION_ADMIN"));
  }

  @Test
  public void testAddRoleMatrix() {
    for (Role userRole : roles) {
      for (Role role : roles) {
        boolean success = testAddRole(role, userRole.getName());
        if (userRole.getName().equals("SUPER_ADMIN")
            || (userRole.getName().equals("ORGANIZATION_ADMIN")
                && !role.getName().equals("SUPER_ADMIN")
                && !role.getName().equals("CONTENT_ADMIN"))) {
          assertTrue(
              success,
              "User " + userRole.getName() + " should be allowed to add role " + role.getName());
        } else {
          assertFalse(
              success,
              "User "
                  + userRole.getName()
                  + " should not be allowed to add role "
                  + role.getName());
        }
      }
    }
  }

  @Test
  public void testRemoveRoleMatrix() {
    for (Role userRole : roles) {
      for (Role role : roles) {
        boolean success = testRemoveRole(role, userRole.getName());
        if (userRole.getName().equals("SUPER_ADMIN")
            || (userRole.getName().equals("ORGANIZATION_ADMIN")
                && !role.getName().equals("SUPER_ADMIN")
                && !role.getName().equals("CONTENT_ADMIN"))) {
          assertTrue(
              success,
              "User " + userRole.getName() + " should be allowed to remove role " + role.getName());
        } else {
          assertFalse(
              success,
              "User "
                  + userRole.getName()
                  + " should not be allowed to remove role "
                  + role.getName());
        }
      }
    }
  }

  private boolean testAddRole(Role role, String userRole) {
    try {
      userService.setUserRoles(
          "5", Collections.singletonList(role.getName()), "4", Collections.singletonList(userRole));
      if (userRole.equals("SUPER_ADMIN")
          || (userRole.equals("ORGANIZATION_ADMIN")
              && !"SUPER_ADMIN".equals(role.getName())
              && !"CONTENT_ADMIN".equals(role.getName()))) {
        verify(keycloakFeign, times(1)).addRoles("5", new Role[] {role});
        Mockito.clearInvocations(keycloakFeign);
        return true;
      } else {
        fail(
            "User "
                + userRole
                + " trying to set role "
                + role.getName()
                + " should throw forbidden exception");
        return false;
      }
    } catch (ForbiddenException e) {
      return false;
    }
  }

  private boolean testRemoveRole(Role role, String userRole) {
    try {
      List<String> allButWantedToRemoveRoles =
          roles.stream()
              .map(Role::getName)
              .filter(name -> !name.equals(role.getName()))
              .collect(Collectors.toList());
      userService.setUserRoles(
          "6", allButWantedToRemoveRoles, "4", Collections.singletonList(userRole));
      if (userRole.equals("SUPER_ADMIN")
          || (userRole.equals("ORGANIZATION_ADMIN")
              && !"SUPER_ADMIN".equals(role.getName())
              && !"CONTENT_ADMIN".equals(role.getName()))) {
        verify(keycloakFeign, times(1)).removeRoles("6", new Role[] {role});
        Mockito.clearInvocations(keycloakFeign);
        return true;
      } else {
        fail("Role setting should throw forbidden exception");
        return false;
      }
    } catch (ForbiddenException e) {
      return false;
    }
  }
}
