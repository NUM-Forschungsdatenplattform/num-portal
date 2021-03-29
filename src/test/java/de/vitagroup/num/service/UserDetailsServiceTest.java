/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
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

  @InjectMocks UserDetailsService userDetailsService;

  @Before
  public void setup() {
    when(userDetailsRepository.findByUserId("existingUserId"))
        .thenReturn(Optional.of(UserDetails.builder().userId("existingUserId").approved(true).build()));
  }

  @Test(expected = ConflictException.class)
  public void shouldHandleExistingUserDetails() {
    userDetailsService.createUserDetails("existingUserId", "unknownEmail");
  }

  @Test
  public void shouldCallRepoWhenCreatingUserDetails() {
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

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingUserWhenApproving() {
    userDetailsService.approveUser("existingUserId", "nonExistingUserId");
  }
}
