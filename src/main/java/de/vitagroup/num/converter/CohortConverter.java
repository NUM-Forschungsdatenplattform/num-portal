package de.vitagroup.num.converter;

import de.vitagroup.num.domain.*;
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

        PropertyMap<CohortGroup, CohortGroupDto> cohortGroupDtoMap = new PropertyMap<>() {
            protected void configure() {
                map().setPhenotypeId(source.getPhenotype().getId());
            }
        };

        PropertyMap<Cohort, CohortDto> cohortDtoMap = new PropertyMap<>() {
            protected void configure() {
                map().setStudyId(source.getStudy().getId());
            }
        };

        modelMapper.addMappings(cohortDtoMap);
        modelMapper.addMappings(cohortGroupDtoMap);
    }

    public CohortDto convertToDto(Cohort cohort) {
        CohortDto cohortDto = modelMapper.map(cohort, CohortDto.class);
        CohortGroupDto cohortGroupDto = convertToCohortGroupDto(cohort.getCohortGroup());
        cohortDto.setCohortGroupDto(cohortGroupDto);
        return cohortDto;
    }

    public Cohort convertToEntity(CohortDto dto) {
        Cohort cohort = modelMapper.map(dto, Cohort.class);
        cohort.setId(null);
        Optional<Study> study = studyService.getStudyById(dto.getStudyId());

        if (study.isPresent()) {
            cohort.setStudy(study.get());
            study.get().setCohort(cohort);
        } else {
            throw new BadRequestException("Invalid study id");
        }

        cohort.setCohortGroup(convertToCohortGroupEntity(dto.getCohortGroupDto()));
        return cohort;
    }

    private CohortGroup convertToCohortGroupEntity(CohortGroupDto dto) {
        CohortGroup cohortGroup = modelMapper.map(dto, CohortGroup.class);
        cohortGroup.setId(null);
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
        if (cohortGroup.getType().equals(Type.GROUP)) {
            dto.setChildren(cohortGroup.getChildren().stream().map(this::convertToCohortGroupDto).collect(Collectors.toList()));
        }
        return dto;
    }

}
