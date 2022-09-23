package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.admin.UserDetails;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, String>, JpaSpecificationExecutor<UserDetails> {

  Optional<UserDetails> findByUserId(String userId);

  Optional<List<UserDetails>> findAllByApproved(boolean approved);
}
