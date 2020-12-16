package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Aql;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AqlRepository extends JpaRepository<Aql, Long> {}
