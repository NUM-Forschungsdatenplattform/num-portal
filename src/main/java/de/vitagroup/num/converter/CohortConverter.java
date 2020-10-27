package de.vitagroup.num.converter;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.service.PhenotypeService;
import de.vitagroup.num.service.StudyService;
import de.vitagroup.num.web.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CohortConverter {

    private final ModelMapper modelMapper;
    private final PhenotypeService phenotypeService;
    private final StudyService studyService;

    @PostConstruct
    public void setUp() {
        PropertyMap<CohortGroup, CohortGroupDto> cohortGroupMap = new PropertyMap<>() {
            protected void configure() {
                map().setPhenotypeId(source.getPhenotype().getId());
            }
        };
        PropertyMap<Cohort, CohortDto> cohortMap = new PropertyMap<>() {
            protected void configure() {
                map().setStudyId(source.getStudy().getId());
            }
        };
        modelMapper.addMappings(cohortGroupMap);
        modelMapper.addMappings(cohortMap);
    }

    public CohortDto convertToDto(Cohort cohort) {
        CohortDto cohortDto = modelMapper.map(cohort, CohortDto.class);
        CohortGroupDto cohortGroupDto = convertToCohortGroupDto(cohort.getCohortGroup());
        cohortDto.setCohortGroup(cohortGroupDto);
        return cohortDto;
    }

    public Cohort convertToEntity(CohortDto dto) {
        Cohort cohort = modelMapper.map(dto, Cohort.class);
        Optional<Study> study = studyService.getStudyById(dto.getStudyId());

        if (study.isPresent()) {
            cohort.setStudy(study.get());
            study.get().setCohort(cohort);
        } else {
            throw new BadRequestException("Invalid study id");
        }

        cohort.setCohortGroup(convertToCohortGroupEntity(dto.getCohortGroup()));
        return cohort;
    }

    private CohortGroup convertToCohortGroupEntity(CohortGroupDto dto) {
        CohortGroup cohortGroup = modelMapper.map(dto, CohortGroup.class);

        if (dto.getType() == Type.PHENOTYPE) {
            Optional<Phenotype> phenotype = phenotypeService.getPhenotypeById(dto.getPhenotypeId());

            if (phenotype.isPresent()) {
                cohortGroup.setPhenotype(phenotype.get());
            } else {
                throw new BadRequestException("Invalid phenotype id");
            }

        }

        if (dto.getType() == Type.GROUP) {
            cohortGroup.setChildren(dto.getChildren().stream().map(child -> {
                CohortGroup cohortGroupChild = convertToCohortGroupEntity(child);
                cohortGroupChild.setParent(cohortGroup);
                return cohortGroupChild;
            }).collect(Collectors.toSet()));
        }

        return cohortGroup;
    }

    private CohortGroupDto convertToCohortGroupDto(CohortGroup cohortGroup) {
        CohortGroupDto dto = modelMapper.map(cohortGroup, CohortGroupDto.class);
        if(cohortGroup.getType().equals(Type.GROUP)){
            dto.setChildren(cohortGroup.getChildren().stream().map(this::convertToCohortGroupDto).collect(Collectors.toList()));
        }
        return dto;
    }

}
