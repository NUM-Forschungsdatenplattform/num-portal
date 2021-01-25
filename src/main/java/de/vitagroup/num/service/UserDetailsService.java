package de.vitagroup.num.service;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.ConflictException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserDetailsService {

  private final UserDetailsRepository userDetailsRepository;

  public Optional<UserDetails> getUserDetailsById(String userId) {
    return userDetailsRepository.findByUserId(userId);
  }

  public UserDetails createUserDetails(String userId) {
    Optional<UserDetails> userDetails = userDetailsRepository.findByUserId(userId);
    if (userDetails.isPresent()) {
      throw new ConflictException("User " + userId + " already exists.");
    } else {
      UserDetails newUserDetails = UserDetails.builder().userId(userId).build();
      return userDetailsRepository.save(newUserDetails);
    }
  }

  public UserDetails setOrganization(String userId, String organizationId) {
    Optional<UserDetails> userDetails = userDetailsRepository.findByUserId(userId);
    if (userDetails.isPresent()) {
      userDetails.get().setOrganizationId(organizationId);
      return userDetailsRepository.save(userDetails.get());
    } else {
      throw new ResourceNotFound("User " + userId + " not created yet.");
    }
  }

  public UserDetails approveUser(String userId) {
    Optional<UserDetails> userDetails = userDetailsRepository.findByUserId(userId);
    return userDetails
        .map(
            details -> {
              details.setApproved(true);
              return userDetailsRepository.save(details);
            })
        .orElseThrow(() -> new ResourceNotFound("User " + userId + " not created yet."));
  }
}
