package org.highmed.numportal.mapper;

import org.highmed.numportal.attachment.service.AttachmentService;
import org.highmed.numportal.domain.dto.ProjectDto;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.service.UserService;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
