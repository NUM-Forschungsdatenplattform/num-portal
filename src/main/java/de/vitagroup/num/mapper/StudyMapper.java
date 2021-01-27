package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.dto.StudyDto;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

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
}
