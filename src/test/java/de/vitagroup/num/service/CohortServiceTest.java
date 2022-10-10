package de.vitagroup.num.service;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.USER_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortAql;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.CohortAqlDto;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.executors.CohortExecutor;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

@RunWith(MockitoJUnitRunner.class)
public class CohortServiceTest {

  @InjectMocks
  private CohortService cohortService;

  @Mock
  private CohortRepository cohortRepository;

  @Mock
  private CohortExecutor cohortExecutor;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private AqlService aqlService;

  @Spy
  private ModelMapper modelMapper;

  @Mock
  private PrivacyProperties privacyProperties;

  @Captor
  ArgumentCaptor<Cohort> cohortCaptor;

  @Captor
  ArgumentCaptor<CohortGroup> cohortGroupCaptor;
  @Captor
  ArgumentCaptor<Map<String, Object>> mapCaptor;
  @Captor
  ArgumentCaptor<Boolean> booleanCaptor;

  private final String Q1 = "SELECT A1 ... FROM E1... WHERE ...";
  private final String Q2 = "SELECT A2 ... FROM E1... WHERE ...";
  private final String NAME1 = "AQL query name 1";
  private final String NAME2 = "AQL query name 2";

  UserDetails approvedUser = UserDetails.builder().userId("approvedUserId").approved(true).build();

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingCohortWhenRetrieving() {
    cohortService.getCohort(1L, "approvedUserId");
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingCohortWhenExecuting() {
    cohortService.executeCohort(1L, false);
  }

  @Test
  public void shouldExecuteCohortExactlyOnce() {
    cohortService.executeCohort(2L, false);
    verify(cohortExecutor, times(1)).execute(any(), anyBoolean());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedUserId() {
    CohortDto cohortDto = CohortDto.builder().build();
    cohortService.createCohort(cohortDto, "notApprovedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedUserIdWhenUpdating() {
    CohortDto cohortDto = CohortDto.builder().build();
    cohortService.updateCohort(cohortDto, 1L, "notApprovedUserId");
  }

  @Test(expected = SystemException.class)
  public void shouldHandleMissingUserId() {
    CohortDto cohortDto = CohortDto.builder().build();
    cohortService.createCohort(cohortDto, "missingUserID");
  }

  @Test(expected = SystemException.class)
  public void shouldHandleMissingUserIdWhenUpdating() {
    CohortDto cohortDto = CohortDto.builder().build();
    cohortService.updateCohort(cohortDto, 1L, "missingUserID");
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingStudy() {
    CohortDto cohortDto = CohortDto.builder().projectId(1L).build();
    cohortService.createCohort(cohortDto, "approvedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleStudyWithDifferentOwner() {
    CohortDto cohortDto = CohortDto.builder().projectId(2L).build();
    cohortService.createCohort(cohortDto, "approvedUserId");
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleNullCohortGroup() {
    CohortDto cohortDto =
        CohortDto.builder().name("Cohort name").projectId(3L).cohortGroup(null).build();
    cohortService.createCohort(cohortDto, "approvedUserId");
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingCohortWhenEditing() {
    CohortDto cohortDto = CohortDto.builder().name("Cohort name").projectId(3L).build();
    cohortService.updateCohort(cohortDto, 3L, "approvedUserId");
  }

  @Test
  public void shouldCorrectlySaveCohort() {
    CohortAqlDto cohortAqlDto1 = CohortAqlDto.builder().id(1L).name(NAME1).query(Q1).build();
    CohortAqlDto cohortAqlDto2 = CohortAqlDto.builder().id(2L).name(NAME2).query(Q2).build();

    CohortGroupDto first = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto1).build();
    CohortGroupDto second = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto2).build();

    CohortGroupDto andCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(List.of(first, second))
            .build();

    CohortDto cohortDto =
        CohortDto.builder().name("Cohort name").projectId(3L).cohortGroup(andCohort).build();

    cohortService.createCohort(cohortDto, "approvedUserId");
    Mockito.verify(cohortRepository).save(cohortCaptor.capture());

    Cohort savedCohort = cohortCaptor.getValue();

    assertThat(savedCohort, notNullValue());
    assertThat(savedCohort.getProject(), notNullValue());
    assertThat(savedCohort.getProject().getId(), is(3L));
    assertThat(savedCohort.getProject().getName(), is("Study name"));
    assertThat(savedCohort.getCohortGroup().getOperator(), is(Operator.AND));
    assertThat(savedCohort.getCohortGroup().getType(), is(Type.GROUP));
    assertThat(savedCohort.getCohortGroup().getChildren().size(), is(2));

    assertThat(
        savedCohort.getCohortGroup().getChildren().stream()
            .anyMatch(c -> c.getQuery().getId() == 1),
        is(true));
    assertThat(
        savedCohort.getCohortGroup().getChildren().stream()
            .anyMatch(c -> c.getQuery().getId() == 2),
        is(true));

    assertThat(
        savedCohort.getCohortGroup().getChildren().stream()
            .allMatch(c -> c.getQuery() instanceof CohortAql),
        is(true));

    assertThat(
        savedCohort.getCohortGroup().getChildren().stream()
            .anyMatch(c -> c.getQuery().getQuery().equals(Q1)),
        is(true));

    assertThat(
        savedCohort.getCohortGroup().getChildren().stream()
            .anyMatch(c -> c.getQuery().getQuery().equals(Q2)),
        is(true));

    assertThat(
        savedCohort.getCohortGroup().getChildren().stream()
            .anyMatch(c -> c.getQuery().getName().equals(NAME1)),
        is(true));

    assertThat(
        savedCohort.getCohortGroup().getChildren().stream()
            .anyMatch(c -> c.getQuery().getName().equals(NAME2)),
        is(true));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailSavingCohortOnApprovedProject() {
    CohortAqlDto cohortAqlDto1 = CohortAqlDto.builder().id(1L).name(NAME1).query(Q1).build();
    CohortAqlDto cohortAqlDto2 = CohortAqlDto.builder().id(2L).name(NAME2).query(Q2).build();

    CohortGroupDto first = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto1).build();
    CohortGroupDto second = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto2).build();

    CohortGroupDto andCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(List.of(first, second))
            .build();

    CohortDto cohortDto =
        CohortDto.builder().name("Cohort name").projectId(4L).cohortGroup(andCohort).build();

    cohortService.createCohort(cohortDto, "approvedUserId");
  }

  @Test
  public void shouldCorrectlyEditCohort() {
    CohortAqlDto cohortAqlDto1 = CohortAqlDto.builder().id(1L).name(NAME1).query(Q1).build();

    CohortGroupDto simpleCohort =
        CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto1).build();

    CohortDto cohortDto =
        CohortDto.builder()
            .name("New cohort name")
            .description("New cohort description")
            .projectId(4L)
            .cohortGroup(simpleCohort)
            .build();

    cohortService.updateCohort(cohortDto, 4L, "approvedUserId");
    Mockito.verify(cohortRepository).save(cohortCaptor.capture());

    Cohort editedCohort = cohortCaptor.getValue();

    assertThat(editedCohort, notNullValue());
    assertThat(editedCohort.getId(), is(4L));
    assertThat(editedCohort.getName(), is("New cohort name"));
    assertThat(editedCohort.getDescription(), is("New cohort description"));
    assertThat(editedCohort.getProject(), notNullValue());
    assertThat(editedCohort.getProject().getId(), is(3L));
    assertThat(editedCohort.getProject().getName(), is("Study name"));
    assertThat(editedCohort.getCohortGroup().getOperator(), nullValue());
    assertThat(editedCohort.getCohortGroup().getType(), is(Type.AQL));
    assertThat(editedCohort.getCohortGroup().getChildren(), nullValue());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailEditingCohortOnApprovedProject() {
    CohortAqlDto cohortAqlDto1 = CohortAqlDto.builder().id(1L).name(NAME1).query(Q1).build();

    CohortGroupDto simpleCohort =
        CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto1).build();

    CohortDto cohortDto =
        CohortDto.builder()
            .name("New cohort name")
            .description("New cohort description")
            .projectId(4L)
            .cohortGroup(simpleCohort)
            .build();

    cohortService.updateCohort(cohortDto, 5L, "approvedUserId");
  }

  @Test
  public void shouldCorrectlyExecuteCohort() {
    CohortAqlDto cohortAqlDto1 = CohortAqlDto.builder().id(1L).name(NAME1).query(Q1).build();
    CohortAqlDto cohortAqlDto2 = CohortAqlDto.builder().id(2L).name(NAME2).query(Q2).build();

    CohortGroupDto first = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto1).build();
    CohortGroupDto second = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto2).build();

    CohortGroupDto orCohort =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.OR)
            .parameters(Map.of("p1", 1))
            .children(List.of(first, second))
            .build();

    long size = cohortService.getCohortGroupSize(orCohort, approvedUser.getUserId(), false);
    Mockito.verify(cohortExecutor, times(1))
        .executeGroup(cohortGroupCaptor.capture(), booleanCaptor.capture());

    assertEquals(2, size);
    CohortGroup executedCohortGroup = cohortGroupCaptor.getValue();
    assertEquals(executedCohortGroup.getOperator(), Operator.OR);
    assertEquals(2, executedCohortGroup.getChildren().size());

    CohortAql cohortAql1 =
        executedCohortGroup.getChildren().stream()
            .filter(cohortGroup -> cohortGroup.getQuery().getId() == 1L)
            .findFirst()
            .get()
            .getQuery();

    CohortAql cohortAql2 =
        executedCohortGroup.getChildren().stream()
            .filter(cohortGroup -> cohortGroup.getQuery().getId() == 2L)
            .findFirst()
            .get()
            .getQuery();

    assertTrue(cohortAql1.getQuery().startsWith(Q1));
    assertTrue(cohortAql2.getQuery().startsWith(Q2));
  }

  @Before
  public void setup() {
    UserDetails notApprovedUser =
        UserDetails.builder().userId("notApprovedUserId").approved(false).build();

    when(userDetailsService.checkIsUserApproved("notApprovedUserId"))
        .thenThrow(new ForbiddenException(CohortServiceTest.class, CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED));

    when(userDetailsService.checkIsUserApproved("missingUserID"))
        .thenThrow(new SystemException(CohortServiceTest.class, USER_NOT_FOUND));

    when(userDetailsService.checkIsUserApproved("approvedUserId")).thenReturn(approvedUser);

    when(projectRepository.findById(2L))
        .thenReturn(
            Optional.of(
                Project.builder()
                    .name("Study")
                    .id(2L)
                    .name("Study name")
                    .coordinator(
                        UserDetails.builder().userId("someOtherUser").approved(true).build())
                    .build()));

    Project ownedProject =
        Project.builder()
            .name("Study")
            .id(3L)
            .name("Study name")
            .coordinator(approvedUser)
            .status(ProjectStatus.DRAFT)
            .build();

    Project ownedProjectApproved =
        Project.builder()
            .name("Study approved")
            .id(4L)
            .name("Study name approved")
            .coordinator(approvedUser)
            .status(ProjectStatus.APPROVED)
            .build();

    when(projectRepository.findById(3L)).thenReturn(Optional.of(ownedProject));
    when(projectRepository.findById(4L)).thenReturn(Optional.of(ownedProjectApproved));

    when(aqlService.existsById(1L)).thenReturn(true);
    when(aqlService.existsById(2L)).thenReturn(true);

    CohortGroup first =
        CohortGroup.builder().type(Type.AQL).query(CohortAql.builder().id(3L).build()).build();
    CohortGroup second =
        CohortGroup.builder().type(Type.AQL).query(CohortAql.builder().id(4L).build()).build();

    CohortGroup andCohort =
        CohortGroup.builder()
            .type(Type.GROUP)
            .operator(Operator.AND)
            .children(List.of(first, second))
            .build();

    Cohort cohortToEdit =
        Cohort.builder()
            .name("Cohort to edit")
            .name("Cohort to edit description")
            .id(4L)
            .project(ownedProject)
            .cohortGroup(andCohort)
            .build();

    Cohort cohortToEditOnApprovedProject =
        Cohort.builder()
            .name("Cohort to edit")
            .name("Cohort to edit description")
            .id(5L)
            .project(ownedProjectApproved)
            .cohortGroup(andCohort)
            .build();

    when(cohortRepository.findById(1L)).thenReturn(Optional.empty());
    when(cohortRepository.findById(2L)).thenReturn(Optional.of(Cohort.builder().id(2L).build()));
    when(cohortRepository.findById(4L)).thenReturn(Optional.of(cohortToEdit));
    when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohortToEditOnApprovedProject));

    when(cohortExecutor.executeGroup(any(), anyBoolean())).thenReturn(Set.of("test1", "test2"));

    when(privacyProperties.getMinHits()).thenReturn(2);
  }
}
