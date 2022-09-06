package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.MailDomain;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.repository.MailDomainRepository;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.domain.specification.OrganizationSpecification;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;

import java.util.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationServiceTest {

  @Mock private OrganizationRepository organizationRepository;

  @Mock private MailDomainRepository mailDomainRepository;

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks private OrganizationService organizationService;

  @Test
  public void shouldHandleInvalidDomainNamesWhenUpdating() {

    List<String> invalidDomains =
        Arrays.asList("*.com", "*.c", "-gmail.com", "gmail.c", "bla", "d.c", "*.a.b.c.c");

    invalidDomains.forEach(
        domain -> {
          Exception exception =
              assertThrows(
                  BadRequestException.class,
                  () ->
                      organizationService.update(
                          3L,
                          OrganizationDto.builder().name("A").mailDomains(Set.of(domain)).build(),
                          List.of(Roles.SUPER_ADMIN),
                          "approvedUserId"));

          String expectedMessage = String.format("%s %s", "Invalid mail domain:", domain);
          assertThat(exception.getMessage(), is(expectedMessage));
        });

    verify(organizationRepository, times(0)).save(any());
  }

  @Test
  public void shouldHandleInvalidDomainNamesWhenCreating() {

    List<String> invalidDomains =
        Arrays.asList("*.com", "*.c", "-gmail.com", "gmail.c", "bla", "d.c", "*.a.b.c.c");

    invalidDomains.forEach(
        domain -> {
          Exception exception =
              assertThrows(
                  BadRequestException.class,
                  () ->
                      organizationService.create(
                          "approvedUserId",
                          OrganizationDto.builder().name("B").mailDomains(Set.of(domain)).build()));

          String expectedMessage = String.format("%s %s", "Invalid mail domain:", domain);
          assertThat(exception.getMessage(), is(expectedMessage));
        });

    verify(organizationRepository, times(0)).save(any());
  }

  @Test
  public void shouldDirectlyResolveOrganization() {
    String email = "john.doe@a.b.c.example.com";
    String domain = "a.b.c.example.com";
    when(mailDomainRepository.findByName(domain))
        .thenReturn(
            Optional.of(
                MailDomain.builder()
                    .name(domain)
                    .organization(Organization.builder().name("A").build())
                    .build()));

    Optional<Organization> organization = organizationService.resolveOrganization(email);

    assertThat(organization.isEmpty(), is(false));
    assertThat(organization.get().getName(), is("A"));
  }

  @Test
  public void shouldResolveOrganization() {
    String email = "john.doe@a.b.c.example.com";
    String domain = "a.b.c.example.com";

    when(mailDomainRepository.findByName(domain)).thenReturn(Optional.empty());
    when(mailDomainRepository.findAll()).thenReturn(createMailDomainsList());

    Optional<Organization> organization = organizationService.resolveOrganization(email);

    assertThat(organization.isEmpty(), is(false));
    assertThat(organization.get().getName(), is("A"));
    assertThat(
        organization.get().getDomains().stream().findFirst().get().getName(),
        is("*.b.c.example.com"));
  }

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
        3L, OrganizationDto.builder().build(), List.of(), "notApprovedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedUserWhenGettingAll() {
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "notApprovedUserId");
  }

  @Test
  public void shouldHandleNoRolesWhenGettingAll() {
    List<Organization> organizations =
        organizationService.getAllOrganizations(List.of(), "approvedUserId");
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
        OrganizationDto.builder().name("Organization").mailDomains(Set.of("vitagroup.ag")).build());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleNotUniqueMailDomainInMixCase() {
    organizationService.create(
        "approvedUserId",
        OrganizationDto.builder().name("Organization").mailDomains(Set.of("vitagroup.ag")).build());
  }

  @Test
  public void shouldSuccessfullySaveOrganization() {
    organizationService.create(
        "approvedUserId",
        OrganizationDto.builder()
            .name("Some organization name")
            .mailDomains(Set.of("*.a.example.com"))
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
            .mailDomains(Set.of("some mail domain name"))
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
            .mailDomains(Set.of("some mail domain name"))
            .build(),
        List.of(),
        "approvedUserId");
  }

  @Test
  public void shouldSaveOrganization() {
    organizationService.update(
        3L,
        OrganizationDto.builder().name("Good name").mailDomains(Set.of("*.example.com")).build(),
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
            .mailDomains(Set.of("some mail domain name", "vitagroup.ag, other organization"))
            .build(),
        List.of(Roles.SUPER_ADMIN),
        "approvedUserId");
    verify(organizationRepository, times(1)).save(any());
  }

  @Test
  public void shouldGetAllOrganizationWithPagination() {
    Pageable pageable = PageRequest.of(0,50);
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "approvedUserId", new SearchCriteria(), pageable);
    verify(organizationRepository, times(1)).findAll(Mockito.any(OrganizationSpecification.class), Mockito.eq(pageable));
  }

  @Test
  public void shouldGetAllOrganizationWithPaginationAndFilter() {
    Pageable pageable = PageRequest.of(1,3);
    ArgumentCaptor<OrganizationSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(OrganizationSpecification.class);
    Map<String, String> filterByName = new HashMap<>();
    filterByName.put("name", "dummy name");
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .filter(filterByName)
            .build();
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "approvedUserId", searchCriteria, pageable);
    verify(organizationRepository, times(1)).findAll(specificationArgumentCaptor.capture(), Mockito.eq(pageable));
    OrganizationSpecification capturedInput = specificationArgumentCaptor.getValue();
    Assert.assertEquals(filterByName, capturedInput.getFilter());
  }

  @Test
  public void shouldGetOrganizationAdminOrganization() {
    Pageable pageable = PageRequest.of(0,25);
    organizationService.getAllOrganizations(List.of(Roles.ORGANIZATION_ADMIN), "approvedUserId", new SearchCriteria(), pageable);
    verify(organizationRepository, times(0)).findAll(Mockito.any(OrganizationSpecification.class), Mockito.eq(pageable));
  }

  @Test
  public void shouldCountOrganizations() {
    organizationService.countOrganizations();
    verify(organizationRepository, times(1)).count();
  }

  @Before
  public void setup() {
    UserDetails approvedUser =
        UserDetails.builder()
            .userId("approvedUserId")
            .approved(true)
            .organization(Organization.builder().name("Organization A").build())
            .build();

    when(userDetailsService.checkIsUserApproved("approvedUserId")).thenReturn(approvedUser);

    when(userDetailsService.checkIsUserApproved("missingUserId"))
        .thenThrow(new SystemException("User not found"));

    when(userDetailsService.checkIsUserApproved("notApprovedUserId"))
        .thenThrow(new ForbiddenException("Cannot access this resource. User is not approved."));

    when(organizationRepository.findByName("Existing organization"))
        .thenReturn(
            Optional.of(Organization.builder().id(5L).name("Existing organization").build()));

    when(organizationRepository.findById(1L)).thenThrow(new ResourceNotFound());

    when(organizationRepository.findById(3L))
        .thenReturn(Optional.of(Organization.builder().id(3L).name("Existing").build()));

    when(mailDomainRepository.findByName("vitagroup.ag"))
        .thenReturn(Optional.of(MailDomain.builder().name("vitagroup.ag").build()));
  }

  private List<MailDomain> createMailDomainsList() {
    List<MailDomain> mailDomains = new LinkedList<>();

    Organization orgc = Organization.builder().name("C").build();
    orgc.setDomains(
        Set.of(MailDomain.builder().organization(orgc).name("*.c.example.com").build()));

    Organization orga = Organization.builder().name("A").build();
    orga.setDomains(
        Set.of(MailDomain.builder().organization(orga).name("*.b.c.example.com").build()));

    Organization orgb = Organization.builder().name("B").build();
    orgb.setDomains(
        Set.of(
            MailDomain.builder().organization(orgb).name("*.a.b.c.example.com").build(),
            MailDomain.builder().organization(orgb).name("c.example.com").build(),
            MailDomain.builder().organization(orgb).name("*.example.com").build()));

    mailDomains.addAll(orgc.getDomains());
    mailDomains.addAll(orgb.getDomains());
    mailDomains.addAll(orga.getDomains());

    return mailDomains;
  }
}
