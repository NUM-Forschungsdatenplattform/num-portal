package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.AqlDto;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@AllArgsConstructor
public class AqlMapper {

  private final ModelMapper modelMapper;

  @PostConstruct
  public void initialize() {

    PropertyMap<Aql, AqlDto> dtoPropertyMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setOwnerId(source.getOwner().getUserId());
          }
        };

    modelMapper.addMappings(dtoPropertyMap);
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
  }

  public AqlDto convertToDto(Aql aql) {
    return modelMapper.map(aql, AqlDto.class);
  }

  public Aql convertToEntity(AqlDto aqlDto) {
    Aql aql = modelMapper.map(aqlDto, Aql.class);
    aql.setId(null);
    return aql;
  }
}
