package org.highmed.numportal.mapper;

import org.highmed.numportal.domain.dto.ProjectViewDto;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.service.UserService;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProjectViewMapper {

  private final ModelMapper modelMapper;
  private final UserService userService;

  public ProjectViewDto convertToDto(Project project) {
    ProjectViewDto projectViewDto = modelMapper.map(project, ProjectViewDto.class);
    User coordinator = userService.getOwner(project.getCoordinator().getUserId());
    projectViewDto.setCoordinator(coordinator);
    return projectViewDto;
  }
}
