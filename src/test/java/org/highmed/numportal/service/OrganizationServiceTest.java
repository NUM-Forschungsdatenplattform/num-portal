package org.highmed.numportal.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.highmed.numportal.domain.dto.OrganizationDto;
import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.model.MailDomain;
import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.MailDomainRepository;
import org.highmed.numportal.domain.repository.OrganizationRepository;
import org.highmed.numportal.domain.specification.OrganizationSpecification;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.ForbiddenException;
import org.highmed.numportal.service.exception.ResourceNotFound;
import org.highmed.numportal.service.exception.SystemException;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationServiceTest {

  @Mock private OrganizationRepository organizationRepository;

  @Mock private MailDomainRepository mailDomainRepository;

  @Mock private UserDetailsService userDetailsService;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

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
              BadRequestException exception =
                      assertThrows(
                              BadRequestException.class,
                              () ->
                                      organizationService.create(
                                              "approvedUserId",
                                              OrganizationDto.builder().name("B").mailDomains(Set.of(domain)).build()));

          String expectedMessage = String.format(INVALID_MAIL_DOMAIN, domain);
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

  @Test
  public void shouldHandleInvalidDomain() {
    Optional<Organization> organization = organizationService.resolveOrganization("invalidEmail");
    assertThat(organization.isEmpty(), is(true));
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
  public void shouldHandleNotApprovedUserWhenGettingPaginated() {
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "notApprovedUserId", new SearchCriteria(), Pageable.ofSize(20));
  }
  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedUserWhenGettingAll() {
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "notApprovedUserId");
  }

  @Test
  public void shouldHandleNoRolesWhenGettingAll() {
    List<Organization> organizations = organizationService.getAllOrganizations(List.of(), "approvedUserId");
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

  @Test
  public void shouldHandleNoRolesWhenGettingAllPaginated() {
    Page<Organization> pageContent =
            organizationService.getAllOrganizations(List.of(), "approvedUserId", new SearchCriteria(), Pageable.ofSize(20));
    assertThat(pageContent.getContent().size(), is(0));
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
            OrganizationDto.builder().name("Organization").mailDomains(Set.of("highmed.org")).build());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleNotUniqueMailDomainInMixCase() {
    organizationService.create(
            "approvedUserId",
            OrganizationDto.builder().name("Organization").mailDomains(Set.of("highmed.org")).build());
  }

  @Test
  public void shouldSuccessfullyCreateOrganization() {
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
  public void shouldUpdateOrganization() {
    organizationService.update(
            3L,
            OrganizationDto.builder()
                    .name("Good name")
                    .mailDomains(Set.of("*.example.com"))
                    .active(Boolean.TRUE)
                    .build(),
            List.of(Roles.SUPER_ADMIN),
            "approvedUserId");
    verify(organizationRepository, times(1)).save(any());
    verify(userDetailsService, times(1)).updateUsersInCache(Mockito.eq(3L));
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleExistingEmailDomain() {
    organizationService.update(
            3L,
            OrganizationDto.builder()
                    .name("Good name")
                    .mailDomains(Set.of("some mail domain name", "highmed.org"))
                    .build(),
            List.of(Roles.SUPER_ADMIN),
            "approvedUserId");
    verify(organizationRepository, Mockito.never()).save(any());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleChangeOwnOrganizationStatus() {
    organizationService.update(
            33L,
            OrganizationDto.builder()
                    .name("New organization name")
                    .active(Boolean.FALSE)
                    .build(),
            List.of(Roles.SUPER_ADMIN),
            "approvedUserId");
    verify(organizationRepository, Mockito.never()).save(any());
  }

  @Test
  public void shouldHandleChangeStatusOrganization() {
    organizationService.update(
            3L,
            OrganizationDto.builder()
                    .name("New good name")
                    .mailDomains(Set.of("*.example.com"))
                    .active(Boolean.FALSE)
                    .build(),
            List.of(Roles.SUPER_ADMIN),
            "approvedUserId");
    verify(organizationRepository, times(1)).save(any());
  }

  @Test
  public void shouldUpdateOrganizationAsOrganizationAdmin() {
    organizationService.update(
            33L,
            OrganizationDto.builder()
                    .name("Other name")
                    .mailDomains(Set.of("*.example.com"))
                    .active(Boolean.TRUE)
                    .build(),
            List.of(Roles.ORGANIZATION_ADMIN),
            "approvedUserId");
    verify(organizationRepository, times(1)).save(any());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleUpdateOtherOrganizationsAsOrganizationAdmin() {
    organizationService.update(
            3L,
            OrganizationDto.builder()
                    .name("Other name")
                    .mailDomains(Set.of("*.example.com"))
                    .active(Boolean.TRUE)
                    .build(),
            List.of(Roles.ORGANIZATION_ADMIN),
            "approvedUserId");
    verify(organizationRepository, Mockito.never()).save(any());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleUpdateOrganizationWithWrongRole() {
    organizationService.update(
            3L,
            OrganizationDto.builder()
                    .name("Other name")
                    .mailDomains(Set.of("*.example.com"))
                    .active(Boolean.TRUE)
                    .build(),
            List.of(Roles.STUDY_APPROVER),
            "approvedUserId");
    verify(organizationRepository, Mockito.never()).save(any());
  }

  @Test
  public void shouldGetAllOrganizationWithPagination() {
    Pageable pageable = PageRequest.of(0,50);
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "approvedUserId", new SearchCriteria(), pageable);
    verify(organizationRepository, times(1)).findAll(Mockito.any(OrganizationSpecification.class), Mockito.eq(pageable));
  }

  @Test
  public void shouldGetAllOrganizationWithPaginationAndFilter() {
    Pageable pageable = PageRequest.of(1,3).withSort(Sort.by(Sort.Direction.DESC, "name"));
    ArgumentCaptor<OrganizationSpecification> specificationArgumentCaptor = ArgumentCaptor.forClass(OrganizationSpecification.class);
    Map<String, String> filterByName = new HashMap<>();
    filterByName.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "dummy name");
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .filter(filterByName)
            .sortBy("name")
            .sort("DESC")
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

  @Test(expected = BadRequestException.class)
  public void shouldHandleInvalidSortWhenGetAllOrganizationWithPagination() {
    Pageable pageable = PageRequest.of(0,50);
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .sort("dummyName")
            .sortBy("ASC")
            .build();
    organizationService.getAllOrganizations(List.of(Roles.SUPER_ADMIN), "approvedUserId", searchCriteria, pageable);
    verify(organizationRepository, never());
  }
  @Test(expected = ResourceNotFound.class)
  public void shouldHandleOrganizationNotFoundWhenDelete() {
    organizationService.deleteOrganization(99L, "approvedUserId");
    Mockito.verify(userDetailsService, never());
    Mockito.verify(organizationRepository, Mockito.never()).deleteById(99L);
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleUsersAssignedWhenDelete() {
    Mockito.when(userDetailsService.countUserDetailsByOrganization(3L)).thenReturn(22L);
    organizationService.deleteOrganization(3L, "approvedUserId");
    Mockito.verify(organizationRepository, Mockito.never()).deleteById(3L);
  }

  @Test
  public void shouldDeleteOrganization() {
    organizationService.deleteOrganization(3L, "approvedUserId");
    Mockito.verify(organizationRepository, Mockito.times(1)).deleteById(Mockito.eq(3L));
    Mockito.verify(userDetailsService, Mockito.times(1)).countUserDetailsByOrganization(Mockito.eq(3L));
  }

  @Test
  public void isAllowedToBeDeletedTest() {
    boolean result = organizationService.isAllowedToBeDeleted(3L);
    Assert.assertTrue(result);
  }

  @Test
  public void getMailDomainsByActiveOrganizationsTest() {
    organizationService.getMailDomainsByActiveOrganizations();
    Mockito.verify(mailDomainRepository, Mockito.times(1)).findAllByActiveOrganization();
  }

  @Before
  public void setup() {
    UserDetails approvedUser =
            UserDetails.builder()
                    .userId("approvedUserId")
                    .approved(true)
                    .organization(Organization.builder().id(33L).name("Organization A").build())
                    .build();

    when(userDetailsService.checkIsUserApproved("approvedUserId")).thenReturn(approvedUser);

    when(userDetailsService.checkIsUserApproved("missingUserId"))
        .thenThrow(new SystemException(OrganizationServiceTest.class, USER_NOT_FOUND));

    when(userDetailsService.checkIsUserApproved("notApprovedUserId"))
        .thenThrow(new ForbiddenException(OrganizationServiceTest.class, CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED));

    when(organizationRepository.findByName("Existing organization"))
        .thenReturn(
            Optional.of(Organization.builder().id(5L).name("Existing organization").build()));

    when(organizationRepository.findById(1L)).thenThrow(new ResourceNotFound(CohortService.class, COHORT_NOT_FOUND, String.format(COHORT_NOT_FOUND, 1L)));

    when(organizationRepository.findById(3L))
        .thenReturn(Optional.of(Organization.builder().id(3L).active(Boolean.TRUE).name("Existing").build()));
    when(organizationRepository.findById(33L))
            .thenReturn(Optional.of(Organization.builder().id(33L).active(Boolean.TRUE).name("Organization name").build()));

    when(mailDomainRepository.findByName("highmed.org"))
        .thenReturn(Optional.of(MailDomain.builder()
                .name("highmed.org")
                        .organization(Organization.builder().id(33L).build())
                .build()));
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
