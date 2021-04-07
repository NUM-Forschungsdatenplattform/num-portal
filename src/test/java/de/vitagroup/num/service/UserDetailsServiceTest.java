package de.vitagroup.num.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.web.exception.ConflictException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.feign.KeycloakFeign;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserDetailsServiceTest {

  @Mock private UserDetailsRepository userDetailsRepository;

  @Mock private KeycloakFeign keycloakFeign;

  @Mock private UserService userService;

  @Mock private NotificationService notificationService;

  @InjectMocks UserDetailsService userDetailsService;

  @Before
  public void setup() {
    when(userDetailsRepository.findByUserId("existingUserId"))
        .thenReturn(
            Optional.of(UserDetails.builder().userId("existingUserId").approved(true).build()));
  }

  @Test(expected = ConflictException.class)
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
}
