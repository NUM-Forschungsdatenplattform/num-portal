package de.vitagroup.num.service;

import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import de.vitagroup.num.web.feign.KeycloakFeign;
import feign.FeignException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

  @Mock private KeycloakFeign keycloakFeign;

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks private UserService userService;

  @Before
  public void setup() {
    when(keycloakFeign.getUser("1")).thenThrow(FeignException.BadRequest.class);
    when(keycloakFeign.getUser("2")).thenThrow(FeignException.InternalServerError.class);
    when(keycloakFeign.getUser("3")).thenThrow(FeignException.NotFound.class);

    when(keycloakFeign.getRolesOfUser("1")).thenThrow(FeignException.BadRequest.class);
    when(keycloakFeign.getRolesOfUser("2")).thenThrow(FeignException.InternalServerError.class);
    when(keycloakFeign.getRolesOfUser("3")).thenThrow(FeignException.NotFound.class);

    when(keycloakFeign.getRole("non-existent role")).thenReturn(null);
    when(keycloakFeign.getRole("handled non-existent role"))
        .thenThrow(FeignException.NotFound.class);
    when(keycloakFeign.getRole("error role")).thenThrow(FeignException.InternalServerError.class);
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
  public void shouldHandleSetNullRole() {
    userService.setUserRole("1", "non-existent role");
  }

  @Test(expected = SystemException.class)
  public void shouldSetRoleError() {
    userService.setUserRole("1", "error role");
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldSetRoleErrorHanddled() {
    userService.setUserRole("1", "handled non-existent role");
  }
}
