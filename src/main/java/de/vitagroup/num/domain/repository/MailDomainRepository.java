package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.MailDomain;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailDomainRepository extends JpaRepository<MailDomain, Long> {

  Optional<MailDomain> findByName(String name);

}
