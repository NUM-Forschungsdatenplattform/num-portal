package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

  Optional<Organization> findByName(String name);

  Page<Organization> findAll(Pageable pageable);

  Page<Organization> findAll(Specification<Organization> specification, Pageable pageable);
}
