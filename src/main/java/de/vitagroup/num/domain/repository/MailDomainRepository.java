package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.model.MailDomain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MailDomainRepository extends JpaRepository<MailDomain, Long> {

  Optional<MailDomain> findByName(String name);

  @Query("SELECT md FROM MailDomain md " +
          "INNER JOIN md.organization org " +
          "WHERE org.active = true")
  List<MailDomain> findAllByActiveOrganization();

}
