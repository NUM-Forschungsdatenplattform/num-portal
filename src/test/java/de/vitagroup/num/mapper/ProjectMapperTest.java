package de.vitagroup.num.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.service.UserService;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

@RunWith(MockitoJUnitRunner.class)
public class ProjectMapperTest {

  @Spy private ModelMapper modelMapper;

  @Mock private TemplateMapper templateMapper;

  @InjectMocks private ProjectMapper projectMapper;

  @Mock
  private UserService userService;

  @Before
  public void setup() {
    projectMapper.initialize();
    when(templateMapper.convertToTemplateInfoDtoList(any()))
        .thenReturn(
            List.of(
                TemplateInfoDto.builder().templateId("param1").name("value1").build(),
                TemplateInfoDto.builder().templateId("param2").name("value2").build()));

    when(userService.getOwner("123")).thenReturn(User.builder().build());
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
    assertThat(dto.getTemplates().stream().anyMatch(c -> c.getTemplateId() == "param1"), is(true));
    assertThat(dto.getTemplates().stream().anyMatch(c -> c.getTemplateId() == "param2"), is(true));
    assertThat(dto.getId(), is(project.getId()));
  }
}
