package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.dto.ProjectViewTO;
import de.vitagroup.num.service.UserService;
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
