package de.vitagroup.num.service;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.List;
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

  public UserDetails createUserDetails(UserDetails userDetails) {
    return userDetailsRepository.save(userDetails);
  }

  public UserDetails createOrUpdateUserDetails(String userId, String organizationId) {
    Optional<UserDetails> userDetails = userDetailsRepository.findByUserId(userId);
    if (userDetails.isPresent()) {
      userDetails.get().setOrganizationId(organizationId);
      return userDetailsRepository.save(userDetails.get());
    } else {
      UserDetails newUserDetails =
          UserDetails.builder().userId(userId).organizationId(organizationId).build();
      return userDetailsRepository.save(newUserDetails);
    }
  }

  public Optional<List<UserDetails>> getUsersByApproved(boolean approved) {
    return userDetailsRepository.findAllByApproved(approved);
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
