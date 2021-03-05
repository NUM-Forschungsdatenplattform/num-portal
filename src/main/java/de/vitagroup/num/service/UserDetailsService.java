package de.vitagroup.num.service;

import de.vitagroup.num.domain.MailDomain;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.MailDomainRepository;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.ConflictException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserDetailsService {

  public static final String DOMAIN_SEPARATOR = "@";
  private final UserDetailsRepository userDetailsRepository;
  private final OrganizationRepository organizationRepository;
  private final MailDomainRepository mailDomainRepository;

  public Optional<UserDetails> getUserDetailsById(String userId) {
    return userDetailsRepository.findByUserId(userId);
  }

  public UserDetails createUserDetails(String userId, String emailAddress) {
    Optional<UserDetails> userDetails = userDetailsRepository.findByUserId(userId);
    if (userDetails.isPresent()) {
      throw new ConflictException("User " + userId + " already exists.");
    } else {
      UserDetails newUserDetails = UserDetails.builder().userId(userId).build();
      resolveOrganization(emailAddress).ifPresent(newUserDetails::setOrganization);
      return userDetailsRepository.save(newUserDetails);
    }
  }

  public UserDetails setOrganization(String loggedInUserId, String userId, Long organizationId) {
    validateAndReturnUserDetails(loggedInUserId);

    UserDetails userDetails =
        userDetailsRepository
            .findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFound("User not found:" + userId));

    Organization organization =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(() -> new ResourceNotFound("Organization not found:" + organizationId));

    userDetails.setOrganization(organization);
    return userDetailsRepository.save(userDetails);
  }

  public UserDetails approveUser(String loggedInUserId, String userId) {

    validateAndReturnUserDetails(loggedInUserId);

    Optional<UserDetails> userDetails = userDetailsRepository.findByUserId(userId);
    return userDetails
        .map(
            details -> {
              details.setApproved(true);
              return userDetailsRepository.save(details);
            })
        .orElseThrow(() -> new ResourceNotFound("User " + userId + " not created yet."));
  }

  public UserDetails validateAndReturnUserDetails(String userId) {
    UserDetails user =
        getUserDetailsById(userId).orElseThrow(() -> new SystemException("User not found"));

    if (user.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. User is not approved.");
    }

    return user;
  }

  private Optional<Organization> resolveOrganization(String email) {
    if (StringUtils.isBlank(email) || !email.contains(DOMAIN_SEPARATOR)) {
      return Optional.empty();
    }
    String domain = email.split("\\" + DOMAIN_SEPARATOR)[1];
    Optional<MailDomain> mailDomain = mailDomainRepository.findByName(domain.toLowerCase());
    return mailDomain.map(MailDomain::getOrganization);
  }
}
