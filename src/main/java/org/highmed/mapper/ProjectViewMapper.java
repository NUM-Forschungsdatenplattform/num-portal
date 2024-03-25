package org.highmed.mapper;

import org.highmed.domain.dto.ProjectViewTO;
import org.highmed.domain.model.Project;
import org.highmed.domain.model.admin.User;
import org.highmed.service.UserService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProjectViewMapper {
    private final ModelMapper modelMapper;
    private final UserService userService;

    public ProjectViewTO convertToDto(Project project) {
        ProjectViewTO projectViewTO = modelMapper.map(project, ProjectViewTO.class);
        User coordinator = userService.getOwner(project.getCoordinator().getUserId());
        projectViewTO.setCoordinator(coordinator);
        return projectViewTO;
    }
}
