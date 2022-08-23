package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.NewUserNotification;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.account.AccountApprovalNotification;
import de.vitagroup.num.service.notification.dto.account.OrganizationUpdateNotification;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.web.feign.KeycloakFeign;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    when(userDetailsRepository.findByUserId("1"))
        .thenReturn(Optional.of(UserDetails.builder().userId("1").approved(true).build()));
    when(userDetailsRepository.findByUserId("2"))
        .thenReturn(Optional.of(UserDetails.builder().userId("2").approved(true).build()));

    when(organizationRepository.findById(3L))
        .thenReturn(Optional.of(Organization.builder().id(3L).name("Organization A").build()));

    when(userService.getUserById("1", false)).thenReturn(User.builder().id("1").build());
    when(userService.getUserById("2", false)).thenReturn(User.builder().id("2").build());

    userDetailsService.setOrganization("1", "2", 3L);

    verify(userDetailsRepository, times(1)).save(any());
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(OrganizationUpdateNotification.class));
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
    when(userService.getByRole(Roles.ORGANIZATION_ADMIN)).thenReturn(new HashSet<>(Arrays.asList(organizationAdmin)));
    userDetailsService.createUserDetails(userId, userEmail);
    verify(notificationService, times(1)).send(notificationCaptor.capture());
    List<Notification> notificationSent = notificationCaptor.getValue();

    assertThat(notificationSent.size(), is(1));
    assertThat(notificationSent.get(0).getClass(), is(NewUserNotification.class));

  }
}
