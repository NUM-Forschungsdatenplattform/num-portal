package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.OrganizationDto;
import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.model.MailDomain;
import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.MailDomainRepository;
import org.highmed.numportal.domain.repository.OrganizationRepository;
import org.highmed.numportal.domain.specification.OrganizationSpecification;
import org.highmed.numportal.events.DeactivateUserEvent;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.ForbiddenException;
import org.highmed.numportal.service.exception.ResourceNotFound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_UPDATE_ORGANIZATION;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.INVALID_MAIL_DOMAIN;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.NOT_ALLOWED_TO_UPDATE_OWN_ORGANIZATION_STATUS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ORGANIZATION_IS_NOT_EMPTY_CANT_DELETE_IT;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ORGANIZATION_MAIL_DOMAIN_ALREADY_EXISTS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ORGANIZATION_MAIL_DOMAIN_CANNOT_BE_NULL_OR_EMPTY;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ORGANIZATION_NAME_MUST_BE_UNIQUE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ORGANIZATION_NOT_FOUND;

/**
 * Service responsible for retrieving organization information from the terminology server
 */
@Slf4j
@Service
@AllArgsConstructor
public class OrganizationService {

  private static final String DOMAIN_SEPARATOR = "@";
  private static final String DOMAIN_VALIDATION_REGEX =
      "^(\\*\\.)?((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
  private final OrganizationRepository organizationRepository;
  private final MailDomainRepository mailDomainRepository;
  private final UserDetailsService userDetailsService;
  private final ApplicationEventPublisher applicationEventPublisher;

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
   * Retrieves a list of all existing email domains that belong to active organizations
   *
   * @return
   */
  public List<String> getMailDomainsByActiveOrganizations() {
    log.info("Load all mail domains from active organizations");
    return mailDomainRepository.findAllByActiveOrganization().stream()
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
   * @param roles
   * @param loggedInUserId
   * @return List with available organizations
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
   *
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
      PageRequest pageRequest = sortBy.map(sort -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort))
          .orElseGet(() -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
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

    validateUniqueOrganizationName(organizationDto.getName(), null);
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

    Organization organization = Organization.builder()
        .name(organizationDto.getName())
        .active(Boolean.TRUE)
        .build();

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
            .orElseThrow(
                () -> new ResourceNotFound(OrganizationService.class, ORGANIZATION_NOT_FOUND, String.format(ORGANIZATION_NOT_FOUND, organizationId)));
    validateStatusChange(organizationId, organizationDto.getActive(), organizationToEdit.getActive(), user);
    validateUniqueOrganizationName(organizationDto.getName(), organizationId);
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

    if (Roles.isSuperAdmin(roles)) {
      updateOrganization(organizationDto, organizationToEdit, loggedInUserId);
    } else if (Roles.isOrganizationAdmin(roles)) {
      if (user.getOrganization().getId().equals(organizationId)) {
        updateOrganization(organizationDto, organizationToEdit, loggedInUserId);
      } else {
        throw new ForbiddenException(
            OrganizationService.class, CANNOT_UPDATE_ORGANIZATION, String.format(CANNOT_UPDATE_ORGANIZATION, organizationId));
      }
    } else {
      throw new ForbiddenException(OrganizationService.class, CANNOT_ACCESS_THIS_RESOURCE);
    }
    return organizationRepository.save(organizationToEdit);
  }

  @Transactional
  public void deleteOrganization(Long organizationId, String loggedInUser) {
    userDetailsService.checkIsUserApproved(loggedInUser);
    if (organizationRepository.findById(organizationId).isEmpty()) {
      throw new ResourceNotFound(OrganizationService.class, ORGANIZATION_NOT_FOUND, String.format(ORGANIZATION_NOT_FOUND, organizationId));
    }
    long assignedUsers = userDetailsService.countUserDetailsByOrganization(organizationId);
    if (assignedUsers != 0) {
      log.error("Not allowed to delete organization {} because has user assigned", organizationId);
      throw new BadRequestException(OrganizationService.class, String.format(ORGANIZATION_IS_NOT_EMPTY_CANT_DELETE_IT, organizationId));
    }
    organizationRepository.deleteById(organizationId);
  }

  public boolean isAllowedToBeDeleted(Long organizationId) {
    if (organizationRepository.findById(organizationId).isEmpty()) {
      throw new ResourceNotFound(OrganizationService.class, ORGANIZATION_NOT_FOUND, String.format(ORGANIZATION_NOT_FOUND, organizationId));
    }
    long assignedUsers = userDetailsService.countUserDetailsByOrganization(organizationId);
    return assignedUsers == 0;
  }

  private void validateMailDomains(Set<String> domains) {
    if (Objects.nonNull(domains)) {
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

  private void validateUniqueOrganizationName(String name, Long organizationId) {
    organizationRepository
        .findByName(name)
        .ifPresent(
            d -> {
              if (Objects.isNull(organizationId) || !d.getId().equals(organizationId)) {
                throw new BadRequestException(OrganizationService.class, ORGANIZATION_NAME_MUST_BE_UNIQUE,
                    String.format(ORGANIZATION_NAME_MUST_BE_UNIQUE, name));
              }
            });
  }

  private void validateStatusChange(Long organizationId, Boolean newStatus, Boolean oldStatus, UserDetails loggedInUser) {
    if (statusChanged(oldStatus, newStatus) && loggedInUser.getOrganization().getId().equals(organizationId)) {
      log.warn("User {} is not allowed to change status for own organization {}", loggedInUser.getUserId(), organizationId);
      throw new ForbiddenException(
          OrganizationService.class, NOT_ALLOWED_TO_UPDATE_OWN_ORGANIZATION_STATUS, NOT_ALLOWED_TO_UPDATE_OWN_ORGANIZATION_STATUS);
    }
  }

  private boolean statusChanged(Boolean oldStatus, Boolean newStatus) {
    return Objects.nonNull(newStatus) && !newStatus.equals(oldStatus);
  }

  private boolean nameChanged(String oldName, String newName) {
    return Objects.nonNull(newName) && !newName.equals(oldName);
  }

  private void updateOrganization(OrganizationDto dto, Organization organization, String loggedInUserId) {
    Boolean oldOrganizationStatus = organization.getActive();
    String oldOrganizationName = organization.getName();
    organization.setName(dto.getName());
    if (Objects.nonNull(dto.getActive())) {
      organization.setActive(dto.getActive());
      if (statusChanged(oldOrganizationStatus, dto.getActive()) && Boolean.FALSE.equals(dto.getActive())) {
        log.info(
            "Organization {} was deactivated by {}, so trigger event to deactivate all users assigned to this organization", organization.getId(),
            loggedInUserId);
        DeactivateUserEvent deactivateUserEvent = new DeactivateUserEvent(this, organization.getId(), loggedInUserId);
        applicationEventPublisher.publishEvent(deactivateUserEvent);
      }
    }

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
    if (nameChanged(oldOrganizationName, organization.getName())) {
      userDetailsService.updateUsersInCache(organization.getId());
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
   * Converts an organization email domain list into a list containing correlation between domains and subdomains and sorts matches descending
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
        throw new BadRequestException(
            OrganizationService.class, String.format("Invalid %s sortBy field for organization", searchCriteria.getSortBy()));
      }
      return Optional.of(Sort.by(
          Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()),
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
