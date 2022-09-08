package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.dto.UserNameDto;
import de.vitagroup.num.mapper.OrganizationMapper;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.account.RolesUpdateNotification;
import de.vitagroup.num.service.notification.dto.account.UserNameUpdateNotification;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;
import de.vitagroup.num.web.feign.KeycloakFeign;
import feign.FeignException;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import feign.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

  @Mock
  private KeycloakFeign keycloakFeign;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private NotificationService notificationService;

  @Spy
  private final ModelMapper modelMapper = new ModelMapper();

  @Spy
  private final OrganizationMapper organizationMapper = new OrganizationMapper(modelMapper);

  @InjectMocks
  private UserService userService;

  @Captor
  ArgumentCaptor<Map<String, Object>> mapArgumentCaptor;

  @Captor
  ArgumentCaptor<String> stringArgumentCaptor;

  @Captor
  ArgumentCaptor<List<Notification>> notificationCaptor;

  private final Set<Role> roles =
      Stream.of(
          new Role("R1", "SUPER_ADMIN"),
          new Role("R2", "RESEARCHER"),
          new Role("R3", "ORGANIZATION_ADMIN"),
          new Role("R4", "STUDY_COORDINATOR"),
          new Role("R5", "CONTENT_ADMIN"),
          new Role("R6", "STUDY_APPROVER")).collect(Collectors.toSet());

  private final Set<User> allValidUsers =
      Sets.newHashSet(
          User.builder().id("4").build(),
          User.builder().id("5").build(),
          User.builder().id("6").build(),
          User.builder().id("7").build(),
          User.builder().id("8").build(),
          User.builder().id("9").build());

  @Before
  public void setup() {
    when(keycloakFeign.getUser("1")).thenThrow(FeignException.BadRequest.class);
    when(keycloakFeign.getUser("2")).thenThrow(FeignException.InternalServerError.class);
    when(keycloakFeign.getUser("3")).thenThrow(FeignException.NotFound.class);

    when(keycloakFeign.getUserRaw(anyString())).thenReturn(new HashMap<>());

    when(keycloakFeign.getRolesOfUser("1")).thenThrow(FeignException.BadRequest.class);
    when(keycloakFeign.getRolesOfUser("2")).thenThrow(FeignException.InternalServerError.class);
    when(keycloakFeign.getRolesOfUser("3")).thenThrow(FeignException.NotFound.class);
    when(keycloakFeign.getRolesOfUser("4")).thenReturn(Stream.of(new Role("R2", "RESEARCHER")).collect(Collectors.toSet()));
    when(keycloakFeign.getRolesOfUser("5")).thenReturn(Collections.emptySet());
    when(keycloakFeign.getRolesOfUser("6")).thenReturn(roles);
    when(keycloakFeign.getRolesOfUser("7")).thenReturn(Set.of(new Role("R2", "RESEARCHER")));
    when(keycloakFeign.getRolesOfUser("8")).thenReturn(Set.of(new Role("R4", "STUDY_COORDINATOR")));
    when(keycloakFeign.getRolesOfUser("9"))
        .thenReturn(Set.of(new Role("R3", "ORGANIZATION_ADMIN")));

    when(keycloakFeign.getRoles()).thenReturn(roles);

    when(keycloakFeign.searchUsers(any(), eq(100000))).thenReturn(allValidUsers);

    when(userDetailsService.getUserDetailsById("4"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("4")
                    .organization(
                        Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                    .approved(true)
                    .build()));

    User user4 =
        User.builder()
            .id("4")
            .organization(OrganizationDto.builder().id(1L).name("org 1").build())
            .approved(true)
            .build();

    User user5 =
        User.builder()
            .id("5")
            .organization(OrganizationDto.builder().id(1L).name("org 1").build())
            .approved(true)
            .build();

    User user6 =
        User.builder()
            .id("6")
            .organization(OrganizationDto.builder().id(1L).name("org 1").build())
            .approved(true)
            .build();

    when(userDetailsService.checkIsUserApproved("4"))
        .thenReturn(
            UserDetails.builder()
                .userId("4")
                .organization(Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                .approved(true)
                .build());

    when(keycloakFeign.getUser("4")).thenReturn(user4);
    when(keycloakFeign.getUser("5")).thenReturn(user5);
    when(keycloakFeign.getUser("6")).thenReturn(user6);

    when(userDetailsService.getUserDetailsById("5"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("5")
                    .organization(
                        Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                    .approved(true)
                    .build()));

    when(userDetailsService.checkIsUserApproved("5"))
        .thenReturn(
            UserDetails.builder()
                .userId("5")
                .organization(Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                .approved(true)
                .build());

    when(userDetailsService.getUserDetailsById("6"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("6")
                    .organization(
                        Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                    .approved(true)
                    .build()));

    when(userDetailsService.getUserDetailsById("7"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("7")
                    .organization(
                        Organization.builder().id(2L).name("org 2").domains(Set.of()).build())
                    .approved(true)
                    .build()));
    when(userDetailsService.checkIsUserApproved("7"))
        .thenReturn(
            UserDetails.builder()
                .userId("7")
                .organization(Organization.builder().id(2L).name("org 2").domains(Set.of()).build())
                .approved(true)
                .build());
    when(userDetailsService.getUserDetailsById("8"))
        .thenReturn(
            Optional.of(
                UserDetails.builder()
                    .userId("8")
                    .organization(
                        Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                    .approved(true)
                    .build()));
    when(userDetailsService.checkIsUserApproved("8")).thenThrow(new ForbiddenException());
    when(userDetailsService.getUserDetailsById("9"))
        .thenReturn(Optional.of(UserDetails.builder().userId("9").approved(true).build()));
    when(userDetailsService.checkIsUserApproved("9"))
        .thenReturn(UserDetails.builder().userId("9").approved(true).build());
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
    when(userDetailsService.checkIsUserApproved("approvedUserId"))
        .thenReturn(UserDetails.builder().userId("approvedUserId").approved(true).build());

    userService.getUserRoles("1", "approvedUserId");
  }

  @Test(expected = SystemException.class)
  public void shouldHandleRolesError() {
    when(userDetailsService.checkIsUserApproved("approvedUserId"))
        .thenReturn(UserDetails.builder().userId("approvedUserId").approved(true).build());

    userService.getUserRoles("2", "approvedUserId");
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleRolesNotFound() {
    when(userDetailsService.checkIsUserApproved("approvedUserId"))
        .thenReturn(UserDetails.builder().userId("approvedUserId").approved(true).build());

    userService.getUserRoles("3", "approvedUserId");
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

    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(RolesUpdateNotification.class));
  }

  @Test
  public void shouldNotSetExistingRole() {
    userService.setUserRoles(
        "4",
        Collections.singletonList(Roles.RESEARCHER),
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

    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(RolesUpdateNotification.class));
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
    Set<User> users = new HashSet<>();
    users.add(User.builder().firstName("John").id("4").build());

    when(keycloakFeign.searchUsers(null, 100000)).thenReturn(users);
    Set<de.vitagroup.num.domain.admin.User> userReturn =
        userService.searchUsers("user", null, null, true, List.of(Roles.SUPER_ADMIN));

    assertThat(userReturn.iterator().next().getRoles().iterator().next(), is("RESEARCHER"));
    verify(keycloakFeign, times(1)).getRolesOfUser("4");
  }

  @Test
  public void shouldReturnEnoughUsers() {
    Set<de.vitagroup.num.domain.admin.User> userReturn =
        userService.searchUsers("user", null, null, false, List.of(Roles.SUPER_ADMIN));
    assertEquals(6, userReturn.size());
  }

  @Test
  public void shouldReturnUserWithoutRoles() {
    Set<User> users = new HashSet<>();
    users.add(User.builder().firstName("John").id("4").build());

    when(keycloakFeign.searchUsers(null, 100000)).thenReturn(users);
    Set<de.vitagroup.num.domain.admin.User> userReturn =
        userService.searchUsers("user", null, null, false, List.of(Roles.SUPER_ADMIN));

    assertNull(userReturn.iterator().next().getRoles());
    verify(keycloakFeign, times(0)).getRolesOfUser("4");
  }

  @Test
  public void shouldHandleMissingOwner() {
    when(keycloakFeign.getUser("missingUserId")).thenThrow(new FeignException.NotFound("", Request.create(Request.HttpMethod.GET, "", new HashMap<>(), null, Charset.defaultCharset(), null), null));
    de.vitagroup.num.domain.admin.User userReturn = userService.getOwner("missingUserId");

    assertNull(userReturn);
    verify(keycloakFeign, times(1)).getUser("missingUserId");
  }

  @Test
  public void shouldReturnUserWithRolesWithinOrg() {
    Set<de.vitagroup.num.domain.admin.User> userReturn =
        userService.searchUsers("5", null, null, false, List.of(Roles.ORGANIZATION_ADMIN));

    assertEquals(4, userReturn.size());
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("4")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("5")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("6")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("8")));
  }

  @Test
  public void shouldReturnUserWithRolesWithinOrgAndResearchers() {
    Set<de.vitagroup.num.domain.admin.User> userReturn =
        userService.searchUsers(
            "5", null, null, false, List.of(Roles.ORGANIZATION_ADMIN, Roles.STUDY_COORDINATOR));
    assertEquals(5, userReturn.size());
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("4")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("5")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("6")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("7")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("8")));
  }

  @Test
  public void shouldNotReturnUsersWithinOrgWhenCallerDoesntHaveOrg() {
    Set<de.vitagroup.num.domain.admin.User> userReturn =
        userService.searchUsers(
            "9", null, null, false, List.of(Roles.ORGANIZATION_ADMIN, Roles.STUDY_COORDINATOR));
    assertEquals(3, userReturn.size());
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("4")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("6")));
    assertTrue(userReturn.stream().anyMatch(user -> user.getId().equals("7")));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldShouldNotAllowOrgAdminSetRolesUserDifferentOrg() {
    userService.setUserRoles(
        "7",
        Collections.singletonList("RESEARCHER"),
        "4",
        Collections.singletonList("ORGANIZATION_ADMIN"));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldNotAllowOrgAdminChangeNameOtherOrgUser() {
    userService.changeUserName(
        "5", new UserNameDto("John", "Doe"), "7", Collections.singletonList("ORGANIZATION_ADMIN"));
  }

  @Test
  public void shouldAllowOrgAdminChangeNameSameOrgUser() {
    userService.changeUserName(
        "5", new UserNameDto("John", "Doe"), "4", Collections.singletonList("ORGANIZATION_ADMIN"));
    verify(keycloakFeign, times(1))
        .updateUser(stringArgumentCaptor.capture(), mapArgumentCaptor.capture());
    Map<String, Object> captured = mapArgumentCaptor.getValue();
    assertEquals("John", captured.get("firstName"));
    assertEquals("Doe", captured.get("lastName"));
    assertEquals("5", stringArgumentCaptor.getValue());

    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(UserNameUpdateNotification.class));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailChangeNameSameOrgUserWithoutOrgAdmin() {
    userService.changeUserName(
        "5", new UserNameDto("John", "Doe"), "4", Collections.singletonList("RESEARCHER"));
  }

  @Test
  public void shouldAllowChangeOwnName() {
    userService.changeUserName("5", new UserNameDto("John", "Doe"), "5", Collections.emptyList());
    verify(keycloakFeign, times(1))
        .updateUser(stringArgumentCaptor.capture(), mapArgumentCaptor.capture());
    Map<String, Object> captured = mapArgumentCaptor.getValue();
    assertEquals("John", captured.get("firstName"));
    assertEquals("Doe", captured.get("lastName"));
    assertEquals("5", stringArgumentCaptor.getValue());

    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(UserNameUpdateNotification.class));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailChangeUnapprovedName() {
    userService.changeUserName(
        "8", new UserNameDto("John", "Doe"), "5", Collections.singletonList("SUPER_ADMIN"));
  }

  @Test(expected = ForbiddenException.class)
  public void unapprovedShouldFailChangeName() {
    userService.changeUserName(
        "5", new UserNameDto("John", "Doe"), "8", Collections.singletonList("SUPER_ADMIN"));
  }

  @Test
  public void shouldAllowSuperAdminChangeNameOtherOrgUser() {
    userService.changeUserName(
        "5", new UserNameDto("John", "Doe"), "7", Collections.singletonList("SUPER_ADMIN"));
    verify(keycloakFeign, times(1))
        .updateUser(stringArgumentCaptor.capture(), mapArgumentCaptor.capture());
    Map<String, Object> captured = mapArgumentCaptor.getValue();
    assertEquals("John", captured.get("firstName"));
    assertEquals("Doe", captured.get("lastName"));
    assertEquals("5", stringArgumentCaptor.getValue());
  }

  @Test
  public void testAddRoleMatrix() {
    for (Role userRole : roles) {
      for (Role role : roles) {
        boolean success = testAddRole(role, userRole.getName());
        if (userRole.getName().equals("SUPER_ADMIN")
            || (userRole.getName().equals("ORGANIZATION_ADMIN")
            && !role.getName().equals("SUPER_ADMIN")
            && !role.getName().equals("CONTENT_ADMIN"))
            && !role.getName().equals("STUDY_APPROVER")) {
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
            && !role.getName().equals("CONTENT_ADMIN")
            && !role.getName().equals("STUDY_APPROVER"))) {
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
