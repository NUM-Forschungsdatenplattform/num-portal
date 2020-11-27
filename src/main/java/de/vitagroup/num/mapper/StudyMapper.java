package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class StudyMapper {

  private final ModelMapper modelMapper;
  private final TemplateMapper templateMapper;

  @PostConstruct
  public void initialize() {
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

    return study;
  }
}
