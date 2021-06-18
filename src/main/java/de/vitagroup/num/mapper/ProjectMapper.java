package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.service.UserService;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProjectMapper {

  private final ModelMapper modelMapper;
  private final TemplateMapper templateMapper;
  private final UserService userService;

  @PostConstruct
  public void initialize() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    PropertyMap<Project, ProjectDto> templatePropertyMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setCohortId(source.getCohort().getId());
          }
        };

    modelMapper.addMappings(templatePropertyMap);
  }

  public ProjectDto convertToDto(Project project) {
    ProjectDto projectDto = modelMapper.map(project, ProjectDto.class);
    projectDto.setTemplates(templateMapper.convertToTemplateInfoDtoList(project.getTemplates()));
    User coordinator = userService.getUserById(project.getCoordinator().getUserId(), false);
    projectDto.setCoordinator(coordinator);
    return projectDto;
  }
}
