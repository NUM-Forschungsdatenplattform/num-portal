package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.MailDomain;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.repository.MailDomainRepository;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationServiceTest {

  @Mock private OrganizationRepository organizationRepository;

  @Mock private MailDomainRepository mailDomainRepository;

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks private OrganizationService organizationService;

  @Test(expected = SystemException.class)
  public void shouldHandleMissingUserWhenCreating() {
    organizationService.create("missingUserId", OrganizationDto.builder().build());
  }

  @Test(expected = SystemException.class)
  public void shouldHandleMissingUserWhenUpdating() {
    organizationService.update(1L, OrganizationDto.builder().build(), List.of(), "missingUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedUserWhenCreating() {
    organizationService.create("notApprovedUserId", OrganizationDto.builder().build());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedUserWhenUpdating() {
    organizationService.update(
        1L, OrganizationDto.builder().build(), List.of(), "notApprovedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedUserWhenGettingAll() {
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "notApprovedUserId");
  }

  @Test
  public void shouldHandleNoRolesWhenGettingAll() {
    List organizations = organizationService.getAllOrganizations(List.of(), "approvedUserId");
    assertThat(organizations.size(), is(0));
  }

  @Test
  public void shouldCorrectlyFilterBasedOnRole1() {
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "approvedUserId");
    verify(organizationRepository, times(1)).findAll();
  }

  @Test
  public void shouldCorrectlyFilterBasedOnRole2() {
    organizationService.getAllOrganizations(List.of(Roles.ORGANIZATION_ADMIN), "approvedUserId");
    verify(organizationRepository, times(0)).findAll();
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleNotUniqueOrganizationName() {
    organizationService.create(
        "approvedUserId", OrganizationDto.builder().name("Existing organization").build());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleNotUniqueMailDomain() {
    organizationService.create(
        "approvedUserId",
        OrganizationDto.builder()
            .name("Organization")
            .mailDomains(Set.of("Existing mail domain"))
            .build());
  }

  @Test
  public void shouldSuccessfullySaveOrganization() {
    organizationService.create(
        "approvedUserId",
        OrganizationDto.builder()
            .name("Some organization name")
            .mailDomains(Set.of("Some mail domain name"))
            .build());

    verify(organizationRepository, times(1)).save(any());
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingOrganizationWhenRetrieving() {
    organizationService.getOrganizationById(1L);
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingOrganizationWhenEditing() {
    organizationService.update(
        2L,
        OrganizationDto.builder()
            .name("Some organization name")
            .mailDomains(Set.of("Some mail domain name"))
            .build(),
        List.of(),
        "approvedUserId");
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleDuplicateNameWhenEditing() {
    organizationService.update(
        3L,
        OrganizationDto.builder()
            .name("Existing organization")
            .mailDomains(Set.of("Some mail domain name"))
            .build(),
        List.of(),
        "approvedUserId");
  }

  @Test
  public void shouldSaveOrganization() {
    organizationService.update(
        3L,
        OrganizationDto.builder()
            .name("Good name")
            .mailDomains(Set.of("Some mail domain name"))
            .build(),
        List.of(Roles.SUPER_ADMIN),
        "approvedUserId");
    verify(organizationRepository, times(1)).save(any());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleExistingEmailDomain() {
    organizationService.update(
        3L,
        OrganizationDto.builder()
            .name("Good name")
            .mailDomains(
                Set.of("Some mail domain name", "Existing mail domain, other organization"))
            .build(),
        List.of(Roles.SUPER_ADMIN),
        "approvedUserId");
    verify(organizationRepository, times(1)).save(any());
  }

  @Before
  public void setup() {
    UserDetails notApprovedUser =
        UserDetails.builder().userId("notApprovedUserId").approved(false).build();

    UserDetails approvedUser =
        UserDetails.builder()
            .userId("approvedUserId")
            .approved(true)
            .organization(Organization.builder().name("Organization A").build())
            .build();

    when(userDetailsService.getUserDetailsById("approvedUserId"))
        .thenReturn(Optional.of(approvedUser));

    when(userDetailsService.getUserDetailsById("notApprovedUserId"))
        .thenReturn(Optional.of(notApprovedUser));

    when(organizationRepository.findByName("Existing organization"))
        .thenReturn(
            Optional.of(Organization.builder().id(5L).name("Existing organization").build()));

    when(organizationRepository.findById(1L)).thenThrow(new ResourceNotFound());

    when(organizationRepository.findById(3L))
        .thenReturn(Optional.of(Organization.builder().id(3L).name("Existing").build()));

    when(mailDomainRepository.findByName("Existing mail domain"))
        .thenReturn(Optional.of(MailDomain.builder().name("Existing mail domain").build()));

    when(mailDomainRepository.findByName("Existing mail domain, other organization"))
        .thenReturn(
            Optional.of(
                MailDomain.builder()
                    .name("Existing mail domain, other organization")
                    .organization(Organization.builder().id(123L).build())
                    .build()));
  }
}