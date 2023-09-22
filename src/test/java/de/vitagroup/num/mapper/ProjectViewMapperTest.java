package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.model.Project;
import de.vitagroup.num.domain.model.ProjectStatus;
import de.vitagroup.num.domain.model.admin.UserDetails;
import de.vitagroup.num.domain.dto.ProjectViewTO;
import de.vitagroup.num.service.UserService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(MockitoJUnitRunner.class)
public class ProjectViewMapperTest {

    @Mock
    private UserService userService;

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private ProjectViewMapper projectViewMapper;

    @Test
    public void shouldCorrectlyConvertProjectToProjectViewTO() {
        Project project = Project.builder()
                .id(1L)
                .name("test project")
                .status(ProjectStatus.PUBLISHED)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .coordinator(UserDetails.builder().userId("userId").build())
                .build();
        ProjectViewTO projectViewTO = projectViewMapper.convertToDto(project);

        Assert.assertNotNull(projectViewTO);
        assertThat(projectViewTO.getId(), is(project.getId()));
        assertThat(projectViewTO.getName(), is(project.getName()));
        assertThat(projectViewTO.getStatus(), is(project.getStatus()));
        assertThat(projectViewTO.getStartDate(), is(project.getStartDate()));
        assertThat(projectViewTO.getEndDate(), is(project.getEndDate()));
        Mockito.verify(userService, Mockito.times(1)).getOwner("userId");
    }
}
