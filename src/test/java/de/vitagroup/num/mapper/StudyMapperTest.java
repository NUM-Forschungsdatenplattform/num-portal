package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.web.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StudyMapperTest {

  @Spy private ModelMapper modelMapper;

  @Mock private UserDetailsService userDetailsService;

  @Mock private TemplateMapper templateMapper;

  @InjectMocks private StudyMapper studyMapper;

  @Before
  public void setup() {
    studyMapper.initialize();
    when(templateMapper.convertToTemplateInfoDtoList(any()))
        .thenReturn(
            List.of(
                TemplateInfoDto.builder().templateId("param1").name("value1").build(),
                TemplateInfoDto.builder().templateId("param2").name("value2").build()));

    when(userDetailsService.getUserDetailsById("12345"))
        .thenReturn(Optional.of(UserDetails.builder().userId("12345").build()));

    when(userDetailsService.getUserDetailsById("notApprovedResearcherId"))
        .thenReturn(
            Optional.of(
                UserDetails.builder().userId("notApprovedResearcherId").approved(false).build()));
  }

  @Test
  public void shouldCorrectlyConvertStudyToStudyDto() {
    Cohort cohort = Cohort.builder().id(1L).build();

    Study study =
        Study.builder()
            .id(1L)
            .name("Study name")
            .cohort(cohort)
            .description("Study description")
            .firstHypotheses("first")
            .secondHypotheses("second")
            .status(StudyStatus.DRAFT)
            .templates(Map.of("param1", "value1", "param2", "value2"))
            .build();

    StudyDto dto = studyMapper.convertToDto(study);

    assertThat(dto, notNullValue());

    assertThat(dto.getName(), is(study.getName()));
    assertThat(dto.getDescription(), is(study.getDescription()));
    assertThat(dto.getFirstHypotheses(), is(study.getFirstHypotheses()));
    assertThat(dto.getSecondHypotheses(), is(study.getSecondHypotheses()));
    assertThat(dto.getCohortId(), is(cohort.getId()));
    assertThat(dto.getTemplates().stream().anyMatch(c -> c.getTemplateId() == "param1"), is(true));
    assertThat(dto.getTemplates().stream().anyMatch(c -> c.getTemplateId() == "param2"), is(true));
    assertThat(dto.getId(), is(study.getId()));
  }

  @Test
  public void shouldCorrectlyConvertStudyToEntity() {

    List<UserDetailsDto> researchers = List.of(UserDetailsDto.builder().userId("12345").build());

    List<TemplateInfoDto> templates = new LinkedList<>();
    templates.add(TemplateInfoDto.builder().templateId("t1").name("n1").build());
    templates.add(TemplateInfoDto.builder().templateId("t2").name("n1").build());

    StudyDto studyDto =
        StudyDto.builder()
            .description("Description")
            .name("Name")
            .templates(templates)
            .firstHypotheses("h1")
            .secondHypotheses("h2")
            .status(StudyStatus.APPROVED)
            .researchers(researchers)
            .build();

    Study study = studyMapper.convertToEntity(studyDto);

    assertThat(study, notNullValue());
    assertThat(study.getName(), is(studyDto.getName()));
    assertThat(study.getDescription(), is(studyDto.getDescription()));
    assertThat(study.getFirstHypotheses(), is(studyDto.getFirstHypotheses()));
    assertThat(study.getSecondHypotheses(), is(studyDto.getSecondHypotheses()));
    assertThat(study.getStatus(), is(studyDto.getStatus()));
    assertThat(study.getStatus(), is(studyDto.getStatus()));

    assertThat(study.getTemplates().get(templates.get(0).getTemplateId()), notNullValue());
    assertThat(study.getTemplates().get(templates.get(1).getTemplateId()), notNullValue());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingResearcher() {

    List<UserDetailsDto> researchers =
        List.of(UserDetailsDto.builder().userId("missingResearcherId").build());

    StudyDto studyDto =
        StudyDto.builder()
            .name("Name")
            .firstHypotheses("h1")
            .status(StudyStatus.APPROVED)
            .researchers(researchers)
            .build();
    studyMapper.convertToEntity(studyDto);
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleNotApprovedResearcher() {

    List<UserDetailsDto> researchers =
        List.of(UserDetailsDto.builder().userId("notApprovedResearcherId").build());

    StudyDto studyDto =
        StudyDto.builder()
            .name("Name")
            .firstHypotheses("h1")
            .status(StudyStatus.APPROVED)
            .researchers(researchers)
            .build();
    studyMapper.convertToEntity(studyDto);
  }
}
