package de.vitagroup.num.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import de.vitagroup.num.web.feign.KeycloakFeign;
import feign.FeignException;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

  @Mock private KeycloakFeign keycloakFeign;

  @InjectMocks private UserService userService;

  private final Set<Role> roles =
      Set.of(
          new Role("R1", "ADMIN"),
          new Role("R2", "RESEARCHER"),
          new Role("R3", "ORGANIZATION_ADMIN"),
          new Role("R4", "STUDY_COORDINATOR"));

  @Before
  public void setup() {
    when(keycloakFeign.getUser("1")).thenThrow(FeignException.BadRequest.class);
    when(keycloakFeign.getUser("2")).thenThrow(FeignException.InternalServerError.class);
    when(keycloakFeign.getUser("3")).thenThrow(FeignException.NotFound.class);

    when(keycloakFeign.getRolesOfUser("1")).thenThrow(FeignException.BadRequest.class);
    when(keycloakFeign.getRolesOfUser("2")).thenThrow(FeignException.InternalServerError.class);
    when(keycloakFeign.getRolesOfUser("3")).thenThrow(FeignException.NotFound.class);
    when(keycloakFeign.getRolesOfUser("4")).thenReturn(Set.of(new Role("R2", "RESEARCHER")));

    when(keycloakFeign.getRoles()).thenReturn(roles);
  }

  @Test(expected = SystemException.class)
  public void shouldHandleGetUserBadRequest() {
    userService.getUserById("1");
  }

  @Test(expected = SystemException.class)
  public void shouldHandleGetUserError() {
    userService.getUserById("2");
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleUserNotFound() {
    userService.getUserById("3");
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
    userService.setUserRoles("4", Collections.singletonList("non-existent role"));
  }

  @Test
  public void shouldAddNewRole() {
    userService.setUserRoles("4", Collections.singletonList("ADMIN"));
    verify(keycloakFeign, times(1)).removeRoles("4", new Role[] {new Role("R2", "RESEARCHER")});
    verify(keycloakFeign, times(1)).addRoles("4", new Role[] {new Role("R1", "ADMIN")});
  }

  @Test
  public void shouldSetExistingRole() {
    userService.setUserRoles("4", Collections.singletonList("RESEARCHER"));
    verify(keycloakFeign, never()).removeRoles(anyString(), any(Role[].class));
    verify(keycloakFeign, times(1)).addRoles("4", new Role[] {new Role("R2", "RESEARCHER")});
  }

  @Test
  public void shouldUnsetRoles() {
    userService.setUserRoles("4", Collections.emptyList());
    verify(keycloakFeign, times(1)).removeRoles("4", new Role[] {new Role("R2", "RESEARCHER")});
    verify(keycloakFeign, never()).addRoles(anyString(), any(Role[].class));
  }
}
