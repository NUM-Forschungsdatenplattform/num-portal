package de.vitagroup.num.mapper;

import de.vitagroup.num.attachment.service.AttachmentService;
import de.vitagroup.num.domain.model.Project;
import de.vitagroup.num.domain.model.admin.User;
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

  private final AttachmentService attachmentService;

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
    User coordinator = userService.getOwner(project.getCoordinator().getUserId());
    projectDto.setCoordinator(coordinator);
    projectDto.setAttachments(attachmentService.findAttachmentsByProjectId(project.getId()));
    return projectDto;
  }
}
