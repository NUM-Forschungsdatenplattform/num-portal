package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.web.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class StudyMapper {

  private final ModelMapper modelMapper;
  private final TemplateMapper templateMapper;
  private final UserDetailsService userDetailsService;

  @PostConstruct
  public void setUp() {
    PropertyMap<Study, StudyDto> templatePropertyMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setCohortId(source.getCohort().getId());
          }
        };

    modelMapper.addMappings(templatePropertyMap);
  }

  public StudyDto convertToDto(Study study) {
    StudyDto studyDto = modelMapper.map(study, StudyDto.class);
    studyDto.setTemplates(templateMapper.convertToTemplateInfoDtoList(study.getTemplates()));
    return studyDto;
  }

  public Study convertToEntity(StudyDto studyDto) {
    Study study = modelMapper.map(studyDto, Study.class);
    study.setId(null);

    if (studyDto.getTemplates() != null) {

      Map<String, String> map =
          studyDto.getTemplates().stream()
              .collect(
                  Collectors.toMap(
                      TemplateInfoDto::getId, TemplateInfoDto::getConcept, (t1, t2) -> t1));

      study.setTemplates(map);
    }

    List<UserDetails> newResearchersList = new LinkedList<>();

    if (studyDto.getResearchers() != null) {
      for (UserDetailsDto dto : studyDto.getResearchers()) {
        Optional<UserDetails> researcher = userDetailsService.getUserDetailsById(dto.getUserId());

        if (researcher.isEmpty()) {
          throw new BadRequestException("Researcher not found");
        }

        if (researcher.get().isNotApproved()) {
          throw new BadRequestException("Researcher not approved");
        }

        newResearchersList.add(researcher.get());
      }
    }

    study.setResearchers(newResearchersList);

    return study;
  }
}
