package de.vitagroup.num.service;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.domain.specification.UserDetailsSpecification;
import de.vitagroup.num.domain.templates.ExceptionsTemplate;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.NewUserNotification;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.account.AccountApprovalNotification;
import de.vitagroup.num.service.notification.dto.account.AccountStatusChangedNotification;
import de.vitagroup.num.service.notification.dto.account.OrganizationUpdateNotification;
import de.vitagroup.num.web.feign.KeycloakFeign;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_ASSIGN_USER_TO_DEACTIVATED_ORGANIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserDetailsServiceTest {

  @Mock private UserDetailsRepository userDetailsRepository;

  @Mock private KeycloakFeign keycloakFeign;

  @Mock private UserService userService;

  @Mock private NotificationService notificationService;

  @Mock private OrganizationService organizationService;

  @Mock private OrganizationRepository organizationRepository;

  @Captor ArgumentCaptor<List<Notification>> notificationCaptor;

  @InjectMocks UserDetailsService userDetailsService;

  @Before
  public void setup() {
    when(userDetailsRepository.findByUserId("existingUserId"))
        .thenReturn(
            Optional.of(UserDetails.builder().userId("existingUserId").approved(true).build()));
  }

  @Test
  public void shouldHandleExistingUserDetails() {
    userDetailsService.createUserDetails("existingUserId", "unknownEmail");
  }

  @Test
  public void shouldCallRepoWhenCreatingUserDetails() {
    User user =
        User.builder()
            .id("newUserId")
            .organization(OrganizationDto.builder().id(1L).name("org 1").build())
            .approved(true)
            .build();
    when(userDetailsRepository.save(any())).thenReturn(UserDetails.builder().build());
    when(userService.getUserById("newUserId", false)).thenReturn(user);

    userDetailsService.createUserDetails("newUserId", "unknownEmail");

    verify(userDetailsRepository, times(1)).save(any());
  }

  @Test
  public void shouldCallRepoWhenWhenSearchingUser() {
    userDetailsService.getUserDetailsById(any());
    verify(userDetailsRepository, times(1)).findByUserId(any());
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingUserWhenSettingOrganization() {
    userDetailsService.setOrganization("existingUserId", "nonExistingUserId", any());
  }

  @Test
  public void shouldSendNotificationWhenSettingOrganization() {
    initializeSetOrganization(Boolean.TRUE);

    verify(userDetailsRepository, times(1)).save(any());
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(OrganizationUpdateNotification.class));
  }

  @Test(expected = ForbiddenException.class)
  public void organizationIsNotActive() {
    initializeSetOrganization(Boolean.FALSE);
  }

  private void initializeSetOrganization(Boolean flag) {
    when(userDetailsRepository.findByUserId("1"))
            .thenReturn(Optional.of(UserDetails.builder().userId("1").approved(true).build()));
    when(userDetailsRepository.findByUserId("2"))
            .thenReturn(Optional.of(UserDetails.builder().userId("2").approved(true).build()));

    when(organizationRepository.findById(3L))
            .thenReturn(Optional.of(Organization.builder().id(3L).name("Organization A").active(flag).build()));

    when(userService.getUserById("1", false)).thenReturn(User.builder().id("1").build());
    when(userService.getUserById("2", false)).thenReturn(User.builder().id("2").build());

    userDetailsService.setOrganization("1", "2", 3L);
  }

  @Test
  public void shouldSendNotificationWhenApprovingUser() {
    when(userDetailsRepository.findByUserId("1"))
        .thenReturn(Optional.of(UserDetails.builder().userId("1").approved(true).build()));
    when(userDetailsRepository.findByUserId("2"))
        .thenReturn(Optional.of(UserDetails.builder().userId("2").approved(true).build()));

    when(userService.getUserById("1", false)).thenReturn(User.builder().id("1").build());
    when(userService.getUserById("2", false)).thenReturn(User.builder().id("2").build());

    userDetailsService.approveUser("1", "2");

    verify(userDetailsRepository, times(1)).save(any());
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(AccountApprovalNotification.class));
  }

  @Test
  public void shouldSendNotificationWhenCreateUserDetails() {
    String userEmail = "dummyUser@vitagroup.de";
    String userId = "dummyUserId";
    Organization organization = Organization.builder()
            .id(9L)
            .name("Organization VitaGroup")
            .build();
    OrganizationDto organizationDto = OrganizationDto.builder()
            .name("Organization VitaGroup")
            .id(9L)
            .build();
    when(organizationService.resolveOrganization(userEmail)).thenReturn(Optional.of(organization));
    User user =
            User.builder()
                    .id(userId)
                    .organization(organizationDto)
                    .approved(false)
                    .email(userEmail)
                    .firstName("user firstname")
                    .lastName("lastname")
                    .build();
    User organizationAdmin = User.builder()
            .firstName("organization admin")
            .lastName("lastname")
            .email("organization-admin@vitagroup.de")
            .organization(organizationDto)
            .build();
    when(userDetailsRepository.save(any())).thenReturn(UserDetails.builder()
            .userId(userId)
            .organization(organization)
            .build());
    when(userService.getUserById(userId, false)).thenReturn(user);
    when(userService.getByRole(Roles.ORGANIZATION_ADMIN)).thenReturn(new HashSet<>(List.of(organizationAdmin)));
    userDetailsService.createUserDetails(userId, userEmail);
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(NewUserNotification.class));
  }

  @Test
  public void getUsersTest() {
    Pageable pageable = PageRequest.of(0, 20);
    UserDetailsSpecification userDetailsSpecification = UserDetailsSpecification.builder().approved(true).build();
    ArgumentCaptor<UserDetailsSpecification> argumentCaptor = ArgumentCaptor.forClass(UserDetailsSpecification.class);
    userDetailsService.getUsers(pageable, userDetailsSpecification);
    Mockito.verify(userDetailsRepository, Mockito.times(1)).findAll(argumentCaptor.capture(), Mockito.eq(pageable));
    Assert.assertEquals(true, argumentCaptor.getValue().getApproved());
  }

  @Test
  public void deleteUserDetailsTest() {
    userDetailsService.deleteUserDetails("user-to-be-removed");
    Mockito.verify(userDetailsRepository, Mockito.times(1)).deleteById(Mockito.eq("user-to-be-removed"));
  }

  @Test
  public void getAllUsersUUIDTest() {
    userDetailsService.getAllUsersUUID();
    Mockito.verify(userDetailsRepository, Mockito.times(1)).getAllUsersId();
  }

  @Test
  public void countUserDetailsTest() {
    userDetailsService.countUserDetails();
    Mockito.verify(userDetailsRepository, Mockito.times(1)).count();
  }

  @Test
  public void shouldHandleNotFoundUserWhenCheckIsUserApproved() {
    try {
      userDetailsService.checkIsUserApproved("not-found-id");
    } catch (SystemException se) {
      Assert.assertTrue(true);
      Assert.assertEquals(ExceptionsTemplate.USER_NOT_FOUND, se.getParamValue());
    }
  }

  @Test
  public void shouldHandleNotApprovedUserWhenCheckIsUserApproved() {
    UserDetails notApproved = UserDetails.builder()
            .userId("not-approved-id")
            .approved(false)
            .build();
    Mockito.when(userDetailsRepository.findByUserId("not-approved-id")).thenReturn(Optional.of(notApproved));
    try {
      userDetailsService.checkIsUserApproved("not-approved-id");
    } catch (ForbiddenException fe) {
      Assert.assertTrue(true);
      Assert.assertEquals(ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED, fe.getMessage());
    }
  }

  @Test
  public void deactivateUsersTest() {
    Organization organization = Organization.builder().id(1L).active(Boolean.TRUE).name("TEST Organization").build();
    UserDetails userOne = UserDetails.builder()
            .userId("user-one")
            .approved(Boolean.TRUE)
            .organization(organization)
            .build();
    UserDetails usertwo = UserDetails.builder()
            .userId("user-two")
            .approved(Boolean.TRUE)
            .organization(organization)
            .build();
    Mockito.when(userDetailsRepository.findByOrganizationId(1L)).thenReturn(List.of(userOne, usertwo));
    userDetailsService.deactivateUsers("loggedInUserId",1L);
    Mockito.verify(userService, Mockito.times(1)).updateUserActiveField(Mockito.eq("loggedInUserId"), Mockito.eq("user-one"), Mockito.eq(Boolean.FALSE));
    Mockito.verify(userService, Mockito.times(1)).updateUserActiveField(Mockito.eq("loggedInUserId"), Mockito.eq("user-two"), Mockito.eq(Boolean.FALSE));
  }

  @Test
  public void sendAccountStatusChangedNotificationsTest() {
    User user =
            User.builder()
                    .id("someUserId")
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@vitagroup.ag")
                    .approved(true)
                    .build();
    User loggedInUser =
            User.builder()
                    .id("loggedInUser")
                    .firstName("Admin")
                    .lastName("Doe")
                    .email("admin.doe@vitagroup.ag")
                    .approved(true)
                    .build();
    when(userService.getUserById("someUserId", false)).thenReturn(user);
    when(userService.getUserById("loggedInUser", false)).thenReturn(loggedInUser);
    userDetailsService.sendAccountStatusChangedNotification("someUserId", "loggedInUser", Boolean.FALSE);
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();
    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(AccountStatusChangedNotification.class));
  }

  @Test
  public void updateUsersInCacheTest() {
    List<String> userIds = List.of("id-1", "id-2", "id-3");
    Mockito.when(userDetailsRepository.findUserIdsByOrganizationIds(Mockito.eq(33L))).thenReturn(userIds);
    userDetailsService.updateUsersInCache(33L);
    verify(userService, times(3)).addUserToCache(Mockito.anyString());
  }
}
