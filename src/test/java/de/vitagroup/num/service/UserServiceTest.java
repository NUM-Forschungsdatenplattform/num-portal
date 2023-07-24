package de.vitagroup.num.service;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.dto.SearchFilter;
import de.vitagroup.num.domain.dto.UserNameDto;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.domain.specification.UserDetailsSpecification;
import de.vitagroup.num.mapper.OrganizationMapper;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.account.RolesUpdateNotification;
import de.vitagroup.num.service.notification.dto.account.UserNameUpdateNotification;
import de.vitagroup.num.web.feign.KeycloakFeign;
import feign.FeignException;
import feign.Request;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.USER_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    @Mock
    private KeycloakFeign keycloakFeign;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserDetailsRepository userDetailsRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private OrganizationMapper organizationMapper;

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

        when(keycloakFeign.getRoles()).thenReturn(roles);

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
                        .enabled(Boolean.TRUE)
                        .build();

        User user5 =
                User.builder()
                        .id("5")
                        .organization(OrganizationDto.builder().id(1L).name("org 1").build())
                        .approved(true)
                        .enabled(Boolean.TRUE)
                        .build();

        User user6 =
                User.builder()
                        .id("6")
                        .organization(OrganizationDto.builder().id(1L).name("org 1").build())
                        .approved(true)
                        .enabled(Boolean.TRUE)
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
        User user7 =
                User.builder()
                        .id("7")
                        .organization(OrganizationDto.builder().id(2L).name("org 2").build())
                        .approved(true)
                        .enabled(Boolean.TRUE)
                        .build();
        Mockito.when(keycloakFeign.getUser("7")).thenReturn(user7);
        when(userDetailsService.checkIsUserApproved("7"))
                .thenReturn(
                        UserDetails.builder()
                                .userId("7")
                                .organization(Organization.builder().id(2L).name("org 2").domains(Set.of()).build())
                                .approved(true)
                                .build());
        when(userDetailsService.checkIsUserApproved("8")).thenThrow(new ForbiddenException());
    }

    @Test
    public void deleteUserEmailNotVerifiedTest() {
        User userToBeRemoved =
                User.builder()
                        .id("user-to-be-removed")
                        .organization(OrganizationDto.builder().id(1L).name("org 1").build())
                        .emailVerified(false)
                        .build();
        Mockito.when(keycloakFeign.getUser("user-to-be-removed")).thenReturn(userToBeRemoved);
        userService.deleteUser("user-to-be-removed", "4");
        Mockito.verify(keycloakFeign, Mockito.times(1)).getUser("user-to-be-removed");
    }

    @Test
    public void deleteUserNotApprovedTest() {
        User userToBeRemoved =
                User.builder()
                        .id("user-to-be-removed")
                        .organization(OrganizationDto.builder().id(1L).name("org 1").build())
                        .emailVerified(false)
                        .build();
        Mockito.when(keycloakFeign.getUser("user-to-be-removed")).thenReturn(userToBeRemoved);
        Mockito.when(userDetailsService.getUserDetailsById("user-to-be-removed"))
                .thenReturn(Optional.of(UserDetails.builder()
                        .userId("user-to-be-removed")
                        .approved(false)
                        .build()));
        userService.deleteUser("user-to-be-removed", "4");
        Mockito.verify(keycloakFeign, Mockito.times(1)).getUser("user-to-be-removed");
        Mockito.verify(userDetailsService, Mockito.times(1)).deleteUserDetails("user-to-be-removed");
    }

    @Test(expected = SystemException.class)
    public void shouldHandleNotAllowedToDeleteEnabledUser() {
        User userToBeRemoved =
                User.builder()
                        .id("user-to-be-removed")
                        .organization(OrganizationDto.builder().id(1L).name("org 1").build())
                        .emailVerified(true)
                        .build();
        Mockito.when(keycloakFeign.getUser("user-to-be-removed")).thenReturn(userToBeRemoved);
        userService.deleteUser("user-to-be-removed", "4");
        Mockito.verify(userDetailsService, Mockito.never()).deleteUserDetails("user-to-be-removed");
    }

    @Test
    public void getUserProfileTest() {
        userService.getUserProfile("4");
        Mockito.verify(keycloakFeign, Mockito.times(1)).getUser("4");
        Mockito.verify(keycloakFeign, Mockito.times(1)).getRolesOfUser("4");
        Mockito.verify(userDetailsService, Mockito.times(1)).getUserDetailsById("4");
    }

    @Test
    public void getUserByIdTest() {
        userService.getUserById("4", true, "4");
        Mockito.verify(userDetailsService, Mockito.times(1)).checkIsUserApproved("4");
        Mockito.verify(keycloakFeign, Mockito.times(1)).getUser("4");
        Mockito.verify(keycloakFeign, Mockito.times(1)).getRolesOfUser("4");
        Mockito.verify(userDetailsService, Mockito.times(1)).getUserDetailsById("4");
    }

    @Test
    public void getByRoleTest() {
        User userOne = User.builder().id("user1").build();
        User userTwo = User.builder().id("4").build();
        Mockito.when(keycloakFeign.getByRole(Roles.RESEARCHER)).thenReturn(new HashSet<>(Arrays.asList(userOne, userTwo)));
        userService.getByRole(Roles.RESEARCHER);
        Mockito.verify(userDetailsService, Mockito.times(2)).getUserDetailsById("4");
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
    public void getUserRolesTestOK() {
        userService.getUserRoles("6", "4");
        Mockito.verify(userDetailsService, Mockito.times(1)).checkIsUserApproved("4");
        Mockito.verify(keycloakFeign, Mockito.times(1)).getRolesOfUser("6");
    }

    @Test
    public void shouldAddNewRole() {
        userService.setUserRoles(
                "4",
                Collections.singletonList("SUPER_ADMIN"),
                "4",
                Collections.singletonList(Roles.SUPER_ADMIN));

        verify(keycloakFeign, times(1)).removeRoles("4", new Role[]{new Role("R2", "RESEARCHER")});
        verify(keycloakFeign, times(1)).addRoles("4", new Role[]{new Role("R1", "SUPER_ADMIN")});

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
        verify(keycloakFeign, never()).addRoles("4", new Role[]{new Role("R2", "RESEARCHER")});
    }

    @Test
    public void shouldUnsetRoles() {
        userService.setUserRoles(
                "4", Collections.emptyList(), "4", Collections.singletonList(Roles.SUPER_ADMIN));
        verify(keycloakFeign, times(1)).removeRoles("4", new Role[]{new Role("R2", "RESEARCHER")});
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
    public void shouldHandleMissingOwner() {
        when(keycloakFeign.getUser("missingUserId")).thenThrow(new FeignException.NotFound("", Request.create(Request.HttpMethod.GET, "", new HashMap<>(), null, Charset.defaultCharset(), null), null, null));
        de.vitagroup.num.domain.admin.User userReturn = userService.getOwner("missingUserId");

        assertNull(userReturn);
        verify(keycloakFeign, times(1)).getUser("missingUserId");
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

    @Test
    public void shouldAllowOrgAdminChangeActiveFlagSameOrgUser() {
        userService.updateUserActiveField(
                "4", "5", false, Collections.singletonList("ORGANIZATION_ADMIN"));
        verify(keycloakFeign, times(1))
                .updateUser(stringArgumentCaptor.capture(), mapArgumentCaptor.capture());
        Map<String, Object> captured = mapArgumentCaptor.getValue();
        assertEquals("false", captured.get("enabled").toString());
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotAllowOrgAdminChangeActiveFlagOtherOrgUser() {
        userService.updateUserActiveField("5", "7", false, Collections.singletonList("ORGANIZATION_ADMIN"));
    }

    @Test(expected = SystemException.class)
    public void shouldHandleKeycloakUserNotFound() {
        Mockito.when(keycloakFeign.getUserRaw("33")).thenReturn(null);
        Mockito.when(userDetailsService.getUserDetailsById("33"))
                .thenReturn(Optional.of(UserDetails.builder()
                        .userId("33")
                        .organization(
                                Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                        .approved(false)
                        .build()));
        userService.updateUserActiveField("4", "33", true, Collections.singletonList("ORGANIZATION_ADMIN"));
    }
    @Test
    public void shouldAllowSuperAdminChangeActiveFlagUser() {
        userService.updateUserActiveField(
                "5", "7", true, Collections.singletonList("SUPER_ADMIN"));
        verify(keycloakFeign, times(1))
                .updateUser(stringArgumentCaptor.capture(), mapArgumentCaptor.capture());
        Map<String, Object> captured = mapArgumentCaptor.getValue();
        assertEquals("true", captured.get("enabled").toString());
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotAllowToChangeOwnActiveFlag() {
        userService.updateUserActiveField("5", "5", false, Collections.singletonList("SUPER_ADMIN"));
    }

    @Test
    public void shouldDeleteUnapprovedUsersAfter30Days() {
        LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC).plusDays(-31);
        getUserDetailsFromRepository(localDateTime);

        userService.deleteUnapprovedUsersAfter30Days();
        Mockito.verify(keycloakFeign, Mockito.times(1)).deleteUser("4");
        Mockito.verify(userDetailsService, Mockito.times(1)).deleteUserDetails("4");
    }

    private void getUserDetailsFromRepository(LocalDateTime localDateTime) {
        when(userDetailsRepository.findAllByApproved(false)).thenReturn(
                Optional.of(List.of(
                        UserDetails.builder()
                                .userId("4")
                                .organization(
                                        Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                                .approved(false)
                                .createdDate(localDateTime)
                                .build())
        ));
    }

    @Test
    public void shouldDeleteUnapprovedUsersAfter30DaysKeycloakCreatedTimestampIsMissing() {
        LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC).plusDays(-31);
        getUserDetailsFromRepository(localDateTime);

        userService.deleteUnapprovedUsersAfter30Days();
    }

    @Test
    public void shouldDeleteUnapprovedUsersAfter30DaysNoUser() {
        userService.deleteUnapprovedUsersAfter30Days();
        Mockito.verify(keycloakFeign, Mockito.never()).deleteUser(Mockito.anyString());
        Mockito.verify(userDetailsService, Mockito.never()).deleteUserDetails(Mockito.anyString());
    }

    @Test
    public void shouldDeleteUnapprovedUsersAfter30DaysResourceNotFound() {
        getUserDetailsFromRepository(null);

        when(keycloakFeign.getUser("4")).thenThrow(new ResourceNotFound(UserService.class, USER_NOT_FOUND, String.format(USER_NOT_FOUND, "4")));
        assertThrows(ResourceNotFound.class,
                () -> userService.deleteUnapprovedUsersAfter30Days());
        Mockito.verify(keycloakFeign, Mockito.never()).deleteUser(Mockito.anyString());
        Mockito.verify(userDetailsService, Mockito.never()).deleteUserDetails(Mockito.anyString());
    }

    @Test
    public void shouldDeleteUnapprovedUsersAfter30DaysBadRequest() {
        getUserDetailsFromRepository(null);

        when(keycloakFeign.getUser("4")).thenThrow(FeignException.BadRequest.class);
        assertThrows(SystemException.class,
                () -> userService.deleteUnapprovedUsersAfter30Days());
    }

    @Test
    public void shouldDeleteUnapprovedUsersAfter30DaysInternalServerError() {
        getUserDetailsFromRepository(null);

        when(keycloakFeign.getUser("4")).thenThrow(FeignException.InternalServerError.class);
        assertThrows(SystemException.class,
                () -> userService.deleteUnapprovedUsersAfter30Days());
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
    @Test
    public void findUsersUUIDFromCacheTest() {
        ConcurrentMapCache usersCache = new ConcurrentMapCache("users", false);
        usersCache.putIfAbsent("user-one", User.builder()
                .id("user-one")
                .firstName("John")
                .lastName("Doe").build());
        usersCache.putIfAbsent("user-two", User.builder()
                .id("user-two")
                .firstName("John")
                .lastName("Foe").build());
        Mockito.when(cacheManager.getCache("users")).thenReturn(usersCache);
        Set<String> result = userService.findUsersUUID("doe");
        Mockito.verify(keycloakFeign, Mockito.never()).searchUsers("doe", 0, 100);
        Assert.assertEquals(1, result.size());
    }

    @Test(expected = BadRequestException.class)
    public void shouldHandleInvalidSortField() {
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .sortBy("DESC")
                .sort("invalid field")
                .build();
        userService.searchUsers("4", List.of(Roles.SUPER_ADMIN), searchCriteria, PageRequest.of(0, 50));
        Mockito.verify(userDetailsService, Mockito.never()).getUsers(Mockito.any(), Mockito.any(UserDetailsSpecification.class));
    }

    @Test
    public void shouldReturnEmptyPageWhenNoUsersFound() {
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "dummy input");
        filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.ORGANIZATION.name());
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .filter(filter)
                .sortBy("email")
                .sort("asc")
                .build();
        mockDataSearchUsers();
        Page<User> response = userService.searchUsers("4", List.of(Roles.SUPER_ADMIN), searchCriteria, PageRequest.of(0, 50));
        Assert.assertTrue(response.isEmpty());
        Mockito.verify(userDetailsService, Mockito.never()).countUserDetails();
        Mockito.verify(userDetailsService, Mockito.never()).getUsers(Mockito.any(PageRequest.class), Mockito.any(UserDetailsSpecification.class));
    }

    @Test
    public void searchUsersWithPaginationAsSuperAdminTest() {
        Pageable pageable = PageRequest.of(0, 50);
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_APPROVED_KEY, "false");
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .filter(filter)
                .sort("asc")
                .sortBy("email")
                .build();
        mockDataSearchUsers();
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
        Page<User> users = userService.searchUsers("4", List.of(Roles.SUPER_ADMIN), searchCriteria, pageable);
        Mockito.verify(keycloakFeign, Mockito.never()).searchUsers(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(PageRequest.of(0,4)), argumentCaptor.capture());
        UserDetailsSpecification capturedInput = argumentCaptor.getValue();
        Assert.assertEquals(Boolean.FALSE, capturedInput.getApproved());
        User firstUser = users.getContent().get(0);
        Assert.assertEquals("ana-maria.doe@vitagroup.ag", firstUser.getEmail());
    }

    private void mockDataSearchUsers() {
        UserDetails userOne = UserDetails.builder()
                .userId("userId-one")
                .organization(
                        Organization.builder().id(99L).name("org 1").domains(Set.of()).build())
                .approved(false)
                .build();
        UserDetails userTwo = UserDetails.builder()
                .userId("userId-two")
                .organization(
                        Organization.builder().id(99L).name("org 1").domains(Set.of()).build())
                .approved(false)
                .build();
        UserDetails user3 = UserDetails.builder()
                .userId("userId-three")
                .organization(
                        Organization.builder().id(89L).name("org 2").domains(Set.of()).build())
                .approved(false)
                .build();
        UserDetails user4 = UserDetails.builder()
                .userId("userId-four")
                .organization(
                        Organization.builder().id(89L).name("org 2").domains(Set.of()).build())
                .approved(false)
                .build();
        ConcurrentMapCache usersCache = new ConcurrentMapCache("users", false);
        usersCache.put("userId-one", User.builder()
                                            .id("userId-one")
                                            .firstName("John")
                                            .lastName("doe")
                                            .email("john.doe@vitagroup.ag")
                                            .createdTimestamp(System.currentTimeMillis())
                                            .roles(Set.of(Roles.RESEARCHER, Roles.STUDY_COORDINATOR, Roles.CRITERIA_EDITOR))
                                            .enabled(true)
                                            .build());
        usersCache.put("userId-two", User.builder()
                .firstName("Ana")
                .lastName("Doe")
                .id("userId-two")
                .email("ana-maria.doe@vitagroup.ag")
                .createdTimestamp(System.currentTimeMillis())
                .roles(Set.of(Roles.ORGANIZATION_ADMIN, Roles.STUDY_COORDINATOR, Roles.CRITERIA_EDITOR))
                .enabled(true)
                .build());
        usersCache.put("userId-three", User.builder()
                .firstName("Elena")
                .lastName("Doe")
                .id("userId-three")
                .email("willi.doe@vitagroup.ag")
                .createdTimestamp(System.currentTimeMillis())
                .roles(Set.of(Roles.RESEARCHER))
                .enabled(false)
                .build());
        usersCache.put("userId-four", User.builder()
                .firstName("Mike")
                .lastName("Doe")
                .id("userId-four")
                .email("mike.doe@vitagroup.ag")
                .createdTimestamp(System.currentTimeMillis())
                .roles(Set.of(Roles.RESEARCHER))
                .enabled(false)
                .build());
        Mockito.when(cacheManager.getCache("users")).thenReturn(usersCache);
        Mockito.when(userDetailsService.countUserDetails()).thenReturn(4L);
        Mockito.when(userDetailsService.getUsers(Mockito.any(Pageable.class), Mockito.any(UserDetailsSpecification.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(userOne, userTwo, user3, user4)));
    }

    @Test
    public void searchUsersWithPaginationAsProjectLeadTest() {
        Pageable pageable = PageRequest.of(0, 50);
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "ana");
        filter.put(SearchCriteria.FILTER_BY_TYPE_KEY, SearchFilter.ORGANIZATION.name());
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .filter(filter)
                .sortBy("lastName")
                .sort("asc")
                .build();
        UserDetails userDetails = UserDetails.builder()
                .userId("userId-two")
                .organization(
                        Organization.builder().id(1L).name("org 1").domains(Set.of()).build())
                .approved(true)
                .build();
        ConcurrentMapCache usersCache = new ConcurrentMapCache("users", false);
        usersCache.put("userId-one", User.builder().firstName("John").lastName("Doe").id("userId-one").build());
        usersCache.put("userId-two", User.builder().firstName("Ana").lastName("John").id("userId-two").build());
        Mockito.when(cacheManager.getCache("users")).thenReturn(usersCache);
        Mockito.when(userDetailsService.countUserDetails()).thenReturn(2L);
        Mockito.when(userDetailsService.getUsers(Mockito.any(Pageable.class), Mockito.any(UserDetailsSpecification.class)))
                .thenReturn(new PageImpl<>(List.of(userDetails)));
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
        userService.searchUsers("4", List.of(Roles.STUDY_COORDINATOR), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(PageRequest.of(0,2)), argumentCaptor.capture());
        UserDetailsSpecification capturedInput = argumentCaptor.getValue();
        Assert.assertNull(capturedInput.getApproved());
        assertThat(1L, is(capturedInput.getLoggedInUserOrganizationId()));
    }

    @Test
    public void searchUsersWithPaginationAsOrganizationAdminTest() {
        Pageable pageable = PageRequest.of(0, 50);
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_USER_WITH_ROLES_KEY, "true");
        filter.put(SearchCriteria.FILTER_BY_ROLES, "ORGANIZATION_ADMIN,RESEARCHER");

        SearchCriteria searchCriteria = SearchCriteria.builder()
                .filter(filter)
                .sort("DESC")
                .sortBy("firstName")
                .build();
        mockDataSearchUsers();
        Mockito.when(userDetailsService.checkIsUserApproved("user-55")).thenReturn(UserDetails.builder()
                .userId("user-55")
                .organization(Organization.builder().id(99L).build())
                .build());
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
        Page<User> userPage = userService.searchUsers("user-55", List.of(Roles.ORGANIZATION_ADMIN), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(PageRequest.of(0,4)), argumentCaptor.capture());
        UserDetailsSpecification capturedInput = argumentCaptor.getValue();
        Assert.assertEquals(99L, capturedInput.getLoggedInUserOrganizationId().longValue());
        User firstUser = userPage.getContent().get(0);
        Assert.assertEquals("Mike", firstUser.getFirstName());
    }

    @Test
    public void searchUsersWithPaginationByRole() {
        Pageable pageable = PageRequest.of(0, 50);
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "RESEARCH");

        SearchCriteria searchCriteria = SearchCriteria.builder()
                .filter(filter)
                .sort("DESC")
                .sortBy("firstName")
                .build();
        mockDataSearchUsers();
        Mockito.when(userDetailsService.checkIsUserApproved("user-55")).thenReturn(UserDetails.builder()
                .userId("user-55")
                .organization(Organization.builder().id(99L).build())
                .build());
        Page<User> userPage = userService.searchUsers("user-55", List.of(Roles.ORGANIZATION_ADMIN), searchCriteria, pageable);
        User firstUser = userPage.getContent().get(0);
        Assert.assertEquals("Mike", firstUser.getFirstName());
    }

    @Test
    public void searchUsersWithPaginationByRoleAndMail() {
        Pageable pageable = PageRequest.of(0, 50);
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_USER_WITH_ROLES_KEY, "true");
        filter.put(SearchCriteria.FILTER_BY_ROLES, "RESEARCHER");
        filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "vitagroup");

        SearchCriteria searchCriteria = SearchCriteria.builder()
                .filter(filter)
                .sort("DESC")
                .sortBy("firstName")
                .build();
        mockDataSearchUsers();
        Mockito.when(userDetailsService.checkIsUserApproved("user-55")).thenReturn(UserDetails.builder()
                .userId("user-55")
                .organization(Organization.builder().id(99L).build())
                .build());
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);

        Mockito.when(userDetailsService.countUserDetails()).thenReturn(2L);
        Page<User> userPage1 = userService.searchUsers("user-55", List.of(Roles.ORGANIZATION_ADMIN), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(PageRequest.of(0,2)), argumentCaptor.capture());
        UserDetailsSpecification capturedInput1 = argumentCaptor.getValue();
        Assert.assertEquals(99L, capturedInput1.getLoggedInUserOrganizationId().longValue());
        User firstUser1 = userPage1.getContent().get(0);
        Assert.assertEquals("Mike", firstUser1.getFirstName());

        Page<User> userPage2 = userService.searchUsers("user-55", List.of(Roles.ORGANIZATION_ADMIN), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(2)).getUsers(Mockito.eq(PageRequest.of(0,2)), argumentCaptor.capture());
        UserDetailsSpecification capturedInput2 = argumentCaptor.getValue();
        Assert.assertEquals(99L, capturedInput2.getLoggedInUserOrganizationId().longValue());
        User firstUser2 = userPage2.getContent().get(1);
        Assert.assertEquals("John", firstUser2.getFirstName());
    }

    @Test
    public void searchUsersFilterByRolesTest() {
        Pageable pageable = PageRequest.of(0, 50);
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_USER_WITH_ROLES_KEY, "true");
        filter.put(SearchCriteria.FILTER_BY_ROLES, "RESEARCHER");
        filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "Doe");
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .filter(filter)
                .sort("ASC")
                .sortBy("firstName")
                .build();
        mockDataSearchUsers();
        Mockito.when(userDetailsService.checkIsUserApproved("user-55")).thenReturn(UserDetails.builder()
                .userId("user-55")
                .organization(Organization.builder().id(99L).build())
                .build());
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
        Page<User> userPage = userService.searchUsers("user-55", List.of(Roles.ORGANIZATION_ADMIN), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(PageRequest.of(0,4)), argumentCaptor.capture());
        UserDetailsSpecification capturedInput = argumentCaptor.getValue();
        Assert.assertEquals(99L, capturedInput.getLoggedInUserOrganizationId().longValue());
        User firstUser = userPage.getContent().get(0);
        Assert.assertEquals("Ana", firstUser.getFirstName());
    }

    @Test
    public void searchUsersWithPaginationAndSortByOrganizationTest() {
        Pageable pageable = PageRequest.of(0, 50);
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .sort("asc")
                .sortBy("organization")
                .build();
        mockDataSearchUsers();
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
        Page<User> users = userService.searchUsers("4", List.of(Roles.SUPER_ADMIN), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(PageRequest.of(0,4)), argumentCaptor.capture());
        User firstUser = users.getContent().get(0);
        Assert.assertEquals("userId-two", firstUser.getId());
    }

    @Test
    public void searchUsersWithPaginationAndSortByRegistrationDateTest() {
        Pageable pageable = PageRequest.of(0, 20);
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .sort("desc")
                .sortBy("registrationDate")
                .build();
        mockDataSearchUsers();
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
        Page<User> users = userService.searchUsers("4", List.of(Roles.SUPER_ADMIN), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(PageRequest.of(0, 4)), argumentCaptor.capture());
        User firstUser = users.getContent().get(0);
        Assert.assertEquals("userId-two", firstUser.getId());
    }

    @Test
    public void searchUsersByKeyAndActiveField() {
        Pageable pageable = PageRequest.of(0, 20);
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_USER_WITH_ROLES_KEY, "true");
        filter.put(SearchCriteria.FILTER_BY_ACTIVE, "false");
        filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "Mike");
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .sort("asc")
                .sortBy("registrationDate")
                .filter(filter)
                .build();
        mockDataSearchUsers();
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
        Page<User> users = userService.searchUsers("4", List.of(Roles.SUPER_ADMIN), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(PageRequest.of(0, 4)), argumentCaptor.capture());
        UserDetailsSpecification captureInput = argumentCaptor.getValue();
        Assert.assertEquals(1, captureInput.getUsersUUID().size());
    }

    @Test
    public void searchUsersByActiveField() {
        Pageable pageable = PageRequest.of(0, 20);
        Map<String, String> filter = new HashMap<>();
        filter.put(SearchCriteria.FILTER_BY_ACTIVE, "true");
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .filter(filter)
                .build();
        mockDataSearchUsers();
        ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
        userService.searchUsers("4", List.of(Roles.SUPER_ADMIN), searchCriteria, pageable);
        Mockito.verify(userDetailsService, Mockito.times(1)).getUsers(Mockito.eq(pageable), argumentCaptor.capture());
        UserDetailsSpecification captureInput = argumentCaptor.getValue();
        Assert.assertEquals(2, captureInput.getUsersUUID().size());
    }

    @Test
    public void initializeUsersCacheTest() {
        List<String> usersUUID = Arrays.asList("4", "5","99");
        ConcurrentMapCache usersCache = new ConcurrentMapCache("users", false);
        Mockito.when(userDetailsService.getAllUsersUUID()).thenReturn(usersUUID);
        Mockito.when(cacheManager.getCache("users")).thenReturn(usersCache);
        Mockito.when(keycloakFeign.getUser("99"))
                .thenThrow(new FeignException.NotFound("user not found",
                        Request.create(Request.HttpMethod.GET, "dummyURL", Collections.emptyMap(), Request.Body.empty(), null), null, null));
        userService.initializeUsersCache();
        Assert.assertEquals(2, usersCache.getNativeCache().size());
    }

    @Test
    public void addUserToCacheTest() {
        userService.addUserToCache("4");
        Mockito.verify(keycloakFeign, Mockito.times(1)).getUser("4");
        Mockito.verify(userDetailsService, Mockito.times(1)).getUserDetailsById("4");
    }

    @Test
    public void refreshUsersCacheTest() {
        ConcurrentMapCache usersCache = new ConcurrentMapCache("users", false);
        usersCache.put("userId-one", User.builder()
                .id("userId-one")
                .firstName("John")
                .lastName("doe")
                .email("john.doe@vitagroup.ag")
                .createdTimestamp(System.currentTimeMillis())
                .build());
        Mockito.when(cacheManager.getCache("users")).thenReturn(usersCache);
        Mockito.when(userDetailsService.getAllUsersUUID()).thenReturn(Arrays.asList("4","5","6"));
        userService.refreshUsersCache();
        Assert.assertNull(usersCache.getNativeCache().get("userId-one"));
        Assert.assertNotNull(usersCache.getNativeCache().get("4"));
        Mockito.verify(userDetailsService, Mockito.times(1)).getAllUsersUUID();
    }

    private boolean testAddRole(Role role, String userRole) {
        try {
            userService.setUserRoles(
                    "5", Collections.singletonList(role.getName()), "4", Collections.singletonList(userRole));
            if (userRole.equals("SUPER_ADMIN")
                    || (userRole.equals("ORGANIZATION_ADMIN")
                    && !"SUPER_ADMIN".equals(role.getName())
                    && !"CONTENT_ADMIN".equals(role.getName()))) {
                verify(keycloakFeign, times(1)).addRoles("5", new Role[]{role});
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
                verify(keycloakFeign, times(1)).removeRoles("6", new Role[]{role});
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
