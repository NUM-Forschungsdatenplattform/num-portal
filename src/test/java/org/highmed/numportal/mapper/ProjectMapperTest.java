package org.highmed.numportal.mapper;

import org.highmed.numportal.attachment.service.AttachmentService;
import org.highmed.numportal.mapper.ProjectMapper;
import org.highmed.numportal.mapper.TemplateMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;
import org.highmed.numportal.domain.dto.ProjectDto;
import org.highmed.numportal.domain.dto.TemplateInfoDto;
import org.highmed.numportal.domain.model.Cohort;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.ProjectStatus;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.service.UserService;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectMapperTest {

  @Spy private ModelMapper modelMapper;

  @Mock private TemplateMapper templateMapper;

  @InjectMocks private ProjectMapper projectMapper;

  @Mock
  private UserService userService;

  @Mock
  private AttachmentService attachmentService;

  @Before
  public void setup() {
    projectMapper.initialize();
    when(templateMapper.convertToTemplateInfoDtoList(any()))
        .thenReturn(
            List.of(
                TemplateInfoDto.builder().templateId("param1").name("value1").build(),
                TemplateInfoDto.builder().templateId("param2").name("value2").build()));

    when(userService.getOwner("123")).thenReturn(User.builder().build());
    when(userService.getOwner("nonExistentUserId")).thenReturn(null);
  }

  @Test
  public void shouldCorrectlyConvertProjectToProjectDto() {
    Cohort cohort = Cohort.builder().id(1L).build();

    Project project =
        Project.builder()
            .id(1L)
            .name("Study name")
            .cohort(cohort)
            .description("Study description")
            .firstHypotheses("first")
            .secondHypotheses("second")
            .status(ProjectStatus.DRAFT)
            .coordinator(UserDetails.builder().userId("123").build())
            .templates(Map.of("param1", "value1", "param2", "value2"))
            .build();

    ProjectDto dto = projectMapper.convertToDto(project);

    assertThat(dto, notNullValue());

    assertThat(dto.getName(), is(project.getName()));
    assertThat(dto.getDescription(), is(project.getDescription()));
    assertThat(dto.getFirstHypotheses(), is(project.getFirstHypotheses()));
    assertThat(dto.getSecondHypotheses(), is(project.getSecondHypotheses()));
    assertThat(dto.getCohortId(), is(cohort.getId()));
    assertThat(dto.getTemplates().stream().anyMatch(c -> c.getTemplateId().equals("param1")), is(true));
    assertThat(dto.getTemplates().stream().anyMatch(c -> c.getTemplateId().equals("param2")), is(true));
    assertThat(dto.getId(), is(project.getId()));
  }

  @Test
  public void shouldCorrectlyConvertProjectToProjectDtoWithNonExistentOwner() {
    Cohort cohort = Cohort.builder().id(1L).build();

    Project project =
            Project.builder()
                    .id(1L)
                    .name("Study name")
                    .cohort(cohort)
                    .description("Study description")
                    .firstHypotheses("first")
                    .secondHypotheses("second")
                    .status(ProjectStatus.DRAFT)
                    .coordinator(UserDetails.builder().userId("nonExistentUserId").build())
                    .templates(Map.of("param1", "value1", "param2", "value2"))
                    .build();

    ProjectDto dto = projectMapper.convertToDto(project);

    assertThat(dto, notNullValue());

    assertThat(dto.getName(), is(project.getName()));
    assertThat(dto.getDescription(), is(project.getDescription()));
    assertThat(dto.getFirstHypotheses(), is(project.getFirstHypotheses()));
    assertThat(dto.getSecondHypotheses(), is(project.getSecondHypotheses()));
    assertThat(dto.getCohortId(), is(cohort.getId()));
    assertThat(dto.getTemplates().stream().anyMatch(c -> c.getTemplateId().equals("param1")), is(true));
    assertThat(dto.getTemplates().stream().anyMatch(c -> c.getTemplateId().equals("param2")), is(true));
    assertThat(dto.getId(), is(project.getId()));
    assertThat(dto.getCoordinator(), nullValue());
  }
}
