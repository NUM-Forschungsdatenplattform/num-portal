package de.vitagroup.num.service;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.ConflictException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.feign.KeycloakFeign;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserDetailsServiceTest {

  @Mock private UserDetailsRepository userDetailsRepository;

  @Mock private KeycloakFeign keycloakFeign;

  @InjectMocks UserDetailsService userDetailsService;

  @Before
  public void setup() {
    when(userDetailsRepository.findByUserId("existingUserId"))
        .thenReturn(Optional.of(UserDetails.builder().userId("existingUserId").build()));
  }

  @Test(expected = ConflictException.class)
  public void shouldHandleExistingUserDetails() {
    userDetailsService.createUserDetails("existingUserId");
  }

  @Test
  public void shouldCallRepoWhenCreatingUserDetails() {
    userDetailsService.createUserDetails("newUserId");
    verify(userDetailsRepository, times(1)).save(any());
  }

  @Test
  public void shouldCallRepoWhenWhenSearchingUser() {
    userDetailsService.getUserDetailsById(any());
    verify(userDetailsRepository, times(1)).findByUserId(any());
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingUserWhenSettingOrganization() {
    userDetailsService.setOrganization("nonExistingUserId", any());
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingUserWhenApproving() {
    userDetailsService.approveUser("nonExistingUserId");
  }
}
