package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  Optional<Organization> findByName(String name);
}
