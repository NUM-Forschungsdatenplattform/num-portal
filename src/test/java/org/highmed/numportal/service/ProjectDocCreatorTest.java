package org.highmed.numportal.service;

import org.highmed.numportal.domain.model.*;
import org.highmed.numportal.service.ProjectDocCreator;
import org.highmed.numportal.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.MessageSource;
import org.highmed.numportal.domain.dto.OrganizationDto;
import org.highmed.numportal.domain.dto.ProjectDto;
import org.highmed.numportal.domain.dto.UserDetailsDto;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.domain.repository.CohortRepository;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDocCreatorTest {

  @Mock
  private UserService userService;
  @Mock
  private CohortRepository cohortRepository;
  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private ProjectDocCreator projectDocCreator;



  private ProjectDto projectDto;

  @Before
  public void setup() {
    projectDto = ProjectDto.builder()
            .id(1L)
            .createDate(OffsetDateTime.now())
            .description("project's description")
            .simpleDescription("some simple description")
            .firstHypotheses("first hypotheses")
            .secondHypotheses("2nd hypotheses")
            .goal("dummy goal")
            .status(ProjectStatus.PUBLISHED)
            .financed(false)
            .usedOutsideEu(true)
            .coordinator(User.builder()
                    .id("user-id")
                    .firstName("John")
                    .lastName("Doe")
                    .organization(OrganizationDto.builder()
                            .id(2L)
                            .name("super duper organization")
                            .build())
                    .build())
            .researchers(List.of(UserDetailsDto.builder()
                            .approved(true)
                            .userId("researcherId")
                    .build()))
            .cohortId(3L)
            .categories(Set.of(ProjectCategories.PREVENTION, ProjectCategories.DECISION_SUPPORT))
            .build();
    Mockito.when(userService.getUserById("researcherId", false))
            .thenReturn(User.builder()
                    .id("reasearcherId")
                    .firstName("Ana")
                    .lastName("Doe")
                    .build());
    Mockito.when(cohortRepository.findById(3L)).thenReturn(Optional.of(Cohort.builder()
                    .name("cohort")
                    .cohortGroup(CohortGroup.builder()
                            .type(Type.AQL)
                            .query(CohortAql.builder()
                                    .name("cohort aql")
                                    .build())
                            .build())
            .build()));

  }

  @Test
  public void getDocBytesOfProject() throws IOException {
    projectDocCreator.getDocBytesOfProject(projectDto, Locale.GERMAN);
    Mockito.verify(messageSource, Mockito.times(1)).getMessage("status.published", null, Locale.GERMAN);
  }

}
