package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Phenotype;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhenotypeRepository extends JpaRepository<Phenotype, Long> {

  List<Phenotype> findByDeletedFalse();
}
