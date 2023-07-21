package de.vitagroup.num.service;

import de.vitagroup.num.domain.MailDomain;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.repository.MailDomainRepository;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.domain.specification.OrganizationSpecification;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.*;

/** Service responsible for retrieving organization information from the terminology server */
@Slf4j
@Service
@AllArgsConstructor
public class OrganizationService {

  private final OrganizationRepository organizationRepository;

  private final MailDomainRepository mailDomainRepository;

  private final UserDetailsService userDetailsService;

  private static final String DOMAIN_SEPARATOR = "@";

  private static final String DOMAIN_VALIDATION_REGEX =
          "^(\\*\\.)?((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";

  /**
   * Resolves organization based on user email address
   *
   * @param email
   * @return
   */
  public Optional<Organization> resolveOrganization(String email) {
    if (StringUtils.isBlank(email) || !email.contains(DOMAIN_SEPARATOR)) {
      return Optional.empty();
    }

    String domain = email.substring(email.indexOf(DOMAIN_SEPARATOR) + 1);

    Optional<MailDomain> mailDomain = mailDomainRepository.findByName(domain.toLowerCase());

    return mailDomain.map(MailDomain::getOrganization).or(() -> matchOrganization(domain));
  }

  /**
   * Retrieves a list of all existing email domains
   *
   * @return
   */
  public List<String> getAllMailDomains() {
    return mailDomainRepository.findAll().stream()
            .map(MailDomain::getName)
            .collect(Collectors.toList());
  }

  /**
   * Counts the number of organization existing in the platform
   *
   * @return
   */
  public long countOrganizations() {
    return organizationRepository.count();
  }

  /**
   * Retrieves a list with available organizations
   *
   * @return List with available organizations
   * @param roles
   * @param loggedInUserId
   */
  public List<Organization> getAllOrganizations(List<String> roles, String loggedInUserId) {
    UserDetails user = userDetailsService.checkIsUserApproved(loggedInUserId);

    if (roles.contains(Roles.SUPER_ADMIN)) {
      return organizationRepository.findAll();
    } else if (roles.contains(Roles.ORGANIZATION_ADMIN)) {
      return List.of(user.getOrganization());
    }
    return List.of();
  }

  /**
   * Retrieves a list with available organizations
   * @param roles
   * @param loggedInUserId
   * @param searchCriteria
   * @param pageable
   * @return
   */
  public Page<Organization> getAllOrganizations(List<String> roles, String loggedInUserId, SearchCriteria searchCriteria, Pageable pageable) {
    UserDetails user = userDetailsService.checkIsUserApproved(loggedInUserId);
    OrganizationSpecification organizationSpecification = new OrganizationSpecification(searchCriteria.getFilter());
    Optional<Sort> sortBy = validateAndGetSort(searchCriteria);
    if (roles.contains(Roles.SUPER_ADMIN)) {
      PageRequest pageRequest = sortBy.map(sort -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)).orElseGet(() -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
      return organizationRepository.findAll(organizationSpecification, pageRequest);
    } else if (roles.contains(Roles.ORGANIZATION_ADMIN)) {
      return new PageImpl<>(List.of(user.getOrganization()));
    }
    return new PageImpl<>(Collections.emptyList());
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
            .orElseThrow(() -> new ResourceNotFound(OrganizationService.class, ORGANIZATION_NOT_FOUND, String.format(ORGANIZATION_NOT_FOUND, id)));
  }

  @Transactional
  public Organization create(String loggedInUserId, OrganizationDto organizationDto) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    organizationRepository
            .findByName(organizationDto.getName())
            .ifPresent(
                    d -> {
                      throw new BadRequestException(Organization.class, ORGANIZATION_NAME_MUST_BE_UNIQUE,
                              String.format(ORGANIZATION_NAME_MUST_BE_UNIQUE, organizationDto.getName()));
                    });
    validateMailDomains(organizationDto.getMailDomains());
    organizationDto
            .getMailDomains()
            .forEach(
                    domain -> {
                      Optional<MailDomain> mailDomain =
                              mailDomainRepository.findByName(domain.toLowerCase());
                      if (mailDomain.isPresent()) {
                        throw new BadRequestException(Organization.class, ORGANIZATION_MAIL_DOMAIN_ALREADY_EXISTS,
                                String.format(ORGANIZATION_MAIL_DOMAIN_ALREADY_EXISTS, organizationDto.getName()));
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

    UserDetails user = userDetailsService.checkIsUserApproved(loggedInUserId);

    Organization organizationToEdit =
            organizationRepository
                    .findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFound(OrganizationService.class, ORGANIZATION_NOT_FOUND, String.format(ORGANIZATION_NOT_FOUND, organizationId)));

    organizationRepository
            .findByName(organizationDto.getName())
            .ifPresent(
                    d -> {
                      if (!d.getId().equals(organizationToEdit.getId())) {
                        throw new BadRequestException(OrganizationService.class, ORGANIZATION_NAME_MUST_BE_UNIQUE,
                                String.format(ORGANIZATION_NAME_MUST_BE_UNIQUE, organizationDto.getName()));
                      }
                    });
    validateMailDomains(organizationDto.getMailDomains());

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
                        throw new BadRequestException(OrganizationService.class, ORGANIZATION_MAIL_DOMAIN_ALREADY_EXISTS,
                                String.format(ORGANIZATION_MAIL_DOMAIN_ALREADY_EXISTS, domain));
                      }
                    });

    if (roles.contains(Roles.SUPER_ADMIN)) {
      updateOrganization(organizationDto, organizationToEdit);

    } else if (roles.contains(Roles.ORGANIZATION_ADMIN)) {
      if (user.getOrganization().getId().equals(organizationId)) {
        updateOrganization(organizationDto, organizationToEdit);
      } else {
        throw new ForbiddenException(OrganizationService.class, CANNOT_UPDATE_ORGANIZATION, String.format(CANNOT_UPDATE_ORGANIZATION, organizationId));
      }
    } else {
      throw new ForbiddenException(OrganizationService.class, CANNOT_ACCESS_THIS_RESOURCE);
    }

    return organizationRepository.save(organizationToEdit);
  }

  @Transactional
  public void deleteOrganization(Long organizationId, String loggedInUser) {
    userDetailsService.checkIsUserApproved(loggedInUser);
    organizationRepository
            .findById(organizationId)
            .orElseThrow(() -> new ResourceNotFound(OrganizationService.class, ORGANIZATION_NOT_FOUND, String.format(ORGANIZATION_NOT_FOUND, organizationId)));
    long assignedUsers = userDetailsService.countUserDetailsByOrganization(organizationId);
    if (assignedUsers != 0) {
      log.error("Not allowed to delete organization {} because has user assigned", organizationId);
      throw new BadRequestException(OrganizationService.class, ORGANIZATION_IS_NOT_EMPTY_CANT_DELETE_IT);
    }
    organizationRepository.deleteById(organizationId);
  }

  private void validateMailDomains(Set<String> domains) {
    if(Objects.nonNull(domains)) {
      domains.forEach(
              domain -> {
                if (StringUtils.isEmpty(domain)) {
                  throw new BadRequestException(OrganizationService.class, ORGANIZATION_MAIL_DOMAIN_CANNOT_BE_NULL_OR_EMPTY);
                }

                if (!Pattern.matches(DOMAIN_VALIDATION_REGEX, domain.toLowerCase())) {
                  throw new BadRequestException(OrganizationService.class, INVALID_MAIL_DOMAIN,
                          String.format(INVALID_MAIL_DOMAIN, domain));
                }
              });
    }
  }

  private void updateOrganization(OrganizationDto dto, Organization organization) {
    organization.setName(dto.getName());

    Set<MailDomain> newDomains = new HashSet<>();

    dto.getMailDomains()
            .forEach(
                    domainName -> {
                      Optional<MailDomain> mailDomain =
                              mailDomainRepository.findByName(domainName.toLowerCase());
                      if (mailDomain.isEmpty()) {
                        newDomains.add(
                                mailDomainRepository.save(
                                        MailDomain.builder()
                                                .name(domainName.toLowerCase())
                                                .organization(organization)
                                                .build()));
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

  private Optional<Organization> matchOrganization(String domain) {
    List<MailDomainDetails> mailDomainDetails = toMailDomainDetails(mailDomainRepository.findAll());

    for (MailDomainDetails mailDomainDetails1 : mailDomainDetails) {
      if (Pattern.matches(mailDomainDetails1.getRegex(), domain)) {
        return Optional.of(mailDomainDetails1.getOrganization());
      }
    }
    return Optional.empty();
  }

  /**
   * Converts an organization email domain list into a list containing correlation between domains
   * and subdomains and sorts matches descending
   *
   * @param allDomains
   * @return
   */
  private List<MailDomainDetails> toMailDomainDetails(List<MailDomain> allDomains) {
    List<MailDomainDetails> mailDomainDetails = new LinkedList<>();

    allDomains.forEach(
            mailDomain ->
                    mailDomainDetails.add(
                            MailDomainDetails.builder()
                                    .domain(mailDomain.getName().toLowerCase())
                                    .regex(domainToRegex(mailDomain.getName().toLowerCase()))
                                    .organization(mailDomain.getOrganization())
                                    .build()));

    for (MailDomainDetails d1 : mailDomainDetails) {
      for (MailDomainDetails d2 : mailDomainDetails) {
        if (Pattern.matches(d2.regex, d1.domain)) {
          d1.matches++;
        }
      }
    }
    mailDomainDetails.sort(Comparator.comparing(MailDomainDetails::getMatches).reversed());
    return mailDomainDetails;
  }

  private String domainToRegex(String domain) {
    String newDomain = domain.replace(".", "\\.");
    return newDomain.replace("*", ".*");
  }

  private Optional<Sort> validateAndGetSort(SearchCriteria searchCriteria) {
    if (searchCriteria.isValid() && StringUtils.isNotEmpty(searchCriteria.getSortBy())) {
      if (!"name".equals(searchCriteria.getSortBy())) {
        throw new BadRequestException(OrganizationService.class, String.format("Invalid %s sortBy field for organization", searchCriteria.getSortBy()));
      }
      return Optional.of(Sort.by(Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()),
              searchCriteria.getSortBy()));
    }
    return Optional.empty();
  }

  @Data
  @Builder
  static class MailDomainDetails {
    String domain;
    String regex;
    int matches;
    Organization organization;
  }
}
