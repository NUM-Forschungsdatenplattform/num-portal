package org.highmed.numportal.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;
import org.highmed.numportal.domain.dto.ProjectViewDto;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.ProjectStatus;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.service.UserService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.LocalDate;

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
        ProjectViewDto projectViewDto = projectViewMapper.convertToDto(project);

        Assert.assertNotNull(projectViewDto);
        assertThat(projectViewDto.getId(), is(project.getId()));
        assertThat(projectViewDto.getName(), is(project.getName()));
        assertThat(projectViewDto.getStatus(), is(project.getStatus()));
        assertThat(projectViewDto.getStartDate(), is(project.getStartDate()));
        assertThat(projectViewDto.getEndDate(), is(project.getEndDate()));
        Mockito.verify(userService, Mockito.times(1)).getOwner("userId");
    }
}
