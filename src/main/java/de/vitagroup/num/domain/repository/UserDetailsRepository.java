package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.admin.UserDetails;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, String>, JpaSpecificationExecutor<UserDetails> {

  Optional<UserDetails> findByUserId(String userId);

  Optional<List<UserDetails>> findAllByApproved(boolean approved);

  @Query("Select ud.userId from UserDetails  ud")
  List<String> getAllUsersId();

  @Query("SELECT COUNT(ud) FROM UserDetails ud WHERE ud.organization.id =:organizationId")
  long countByOrganization(@Param("organizationId") Long organizationId);
}
