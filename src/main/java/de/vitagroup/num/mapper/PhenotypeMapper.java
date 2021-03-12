package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.dto.ExpressionDto;
import de.vitagroup.num.domain.dto.PhenotypeDto;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PhenotypeMapper {

  private final ModelMapper modelMapper;

  @PostConstruct
  public void initialize() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
  }

  public PhenotypeDto convertToDto(Phenotype phenotype) {
    PhenotypeDto phenotypeDto = modelMapper.map(phenotype, PhenotypeDto.class);
    if (phenotype.getOwner() != null) {
      phenotypeDto.setOwnerId(phenotype.getOwner().getUserId());
    }
    return phenotypeDto;
  }

  public Phenotype convertToEntity(PhenotypeDto phenotypeDto) {
    Phenotype phenotype = modelMapper.map(phenotypeDto, Phenotype.class);
    phenotype.setId(null);
    return phenotype;
  }

  public Phenotype convertToEntity(ExpressionDto expressionDto) {
    return modelMapper.map(expressionDto, Phenotype.class);
  }
}
