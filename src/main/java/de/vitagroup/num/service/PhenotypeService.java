package de.vitagroup.num.service;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.repository.PhenotypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PhenotypeService {

    private final PhenotypeRepository phenotypeRepository;

    public Optional<Phenotype> getPhenotypeById(Long phenotypeId) {
        return phenotypeRepository.findById(phenotypeId);
    }

    public List<Phenotype> getAllPhenotypes() {
        return phenotypeRepository.findAll();
    }

    public Phenotype createPhenotypes(Phenotype phenotype) {
        return phenotypeRepository.save(phenotype);
    }
}
