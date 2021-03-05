package de.vitagroup.num.service;

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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service responsible for retrieving organization information from the terminology server */
@Slf4j
@Service
@AllArgsConstructor
public class OrganizationService {

  private final OrganizationRepository organizationRepository;
  private final MailDomainRepository mailDomainRepository;
  private final UserDetailsService userDetailsService;

  /**
   * Retrieves a list with available organizations
   *
   * @return List with available organizations
   */
  public List<Organization> getAllOrganizations(List<String> roles, String loggedInUserId) {
    UserDetails user = userDetailsService.validateAndReturnUserDetails(loggedInUserId);

    if (roles.contains(Roles.SUPER_ADMIN)) {
      return organizationRepository.findAll();
    } else if (roles.contains(Roles.ORGANIZATION_ADMIN)) {
      return List.of(user.getOrganization());
    }
    return List.of();
  }

  /**
   * Retrieves an organization by external identifier
   *
   * @param id the id of the organization to fetch
   * @return the found organization. If none is found, ResourceNotFound exception is thrown
   */
  public Organization getOrganizationById(Long id) {

    return organizationRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFound("Organization not found:" + id));
  }

  @Transactional
  public Organization create(String loggedInUserId, OrganizationDto organizationDto) {
    userDetailsService.validateAndReturnUserDetails(loggedInUserId);

    organizationRepository
        .findByName(organizationDto.getName())
        .ifPresent(
            d -> {
              throw new BadRequestException(
                  "Organization name must be unique: " + organizationDto.getName());
            });

    organizationDto
        .getMailDomains()
        .forEach(
            domain -> {
              Optional<MailDomain> mailDomain =
                  mailDomainRepository.findByName(domain.toLowerCase());
              if (mailDomain.isPresent()) {
                throw new BadRequestException("Organization mail domain already exists: " + domain);
              }
            });

    Organization organization = Organization.builder().name(organizationDto.getName()).build();

    organization.setDomains(
        organizationDto.getMailDomains().stream()
            .map(
                domain ->
                    MailDomain.builder()
                        .name(domain.toLowerCase())
                        .organization(organization)
                        .build())
            .collect(Collectors.toSet()));

    return organizationRepository.save(organization);
  }

  @Transactional
  public Organization update(
      Long organizationId,
      OrganizationDto organizationDto,
      List<String> roles,
      String loggedInUserId) {

    UserDetails user = userDetailsService.validateAndReturnUserDetails(loggedInUserId);

    Organization organizationToEdit =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(() -> new ResourceNotFound("Organization not found: " + organizationId));

    organizationRepository
        .findByName(organizationDto.getName())
        .ifPresent(
            d -> {
              if (!d.getId().equals(organizationToEdit.getId())) {
                throw new BadRequestException(
                    "Organization name must be unique: " + organizationDto.getName());
              }
            });

    organizationDto
        .getMailDomains()
        .forEach(
            domain -> {
              Optional<MailDomain> mailDomain =
                  mailDomainRepository.findByName(domain.toLowerCase());
              if (mailDomain.isPresent()
                  && !mailDomain
                  .get()
                  .getOrganization()
                  .getId()
                  .equals(organizationToEdit.getId())) {
                throw new BadRequestException("Organization mail domain already exists: " + domain);
              }
            });

    if (roles.contains(Roles.SUPER_ADMIN)) {
      updateOrganization(organizationDto, organizationToEdit);

    } else if (roles.contains(Roles.ORGANIZATION_ADMIN)) {
      if (user.getOrganization().getId().equals(organizationId)) {
        updateOrganization(organizationDto, organizationToEdit);
      } else {
        throw new ForbiddenException("Cannot update organization:" + organizationId);
      }
    } else {
      throw new ForbiddenException("Cannot access this resource");
    }

    return organizationRepository.save(organizationToEdit);
  }

  private void updateOrganization(OrganizationDto dto, Organization organization) {
    organization.setName(dto.getName());

    Set<MailDomain> newDomains = new HashSet<>();

    dto.getMailDomains()
        .forEach(
            domainName -> {
              Optional<MailDomain> mailDomain = mailDomainRepository
                  .findByName(domainName.toLowerCase());
              if (mailDomain.isEmpty()) {
                newDomains.add(
                    mailDomainRepository.save(
                        MailDomain.builder().name(domainName.toLowerCase())
                            .organization(organization).build()));
              } else {
                newDomains.add(mailDomain.get());
              }
            });

    if (organization.getDomains() != null) {
      organization.getDomains().clear();
      organization.getDomains().addAll(newDomains);
    } else {
      organization.setDomains(newDomains);
    }
  }
}
