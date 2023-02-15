package de.vitagroup.num.service;

import de.vitagroup.num.domain.*;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.*;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.domain.templates.ExceptionsTemplate;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.*;
import de.vitagroup.num.service.executors.CohortExecutor;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.util.*;

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
import static org.mockito.Mockito.*;

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

    @Mock
    private TemplateService templateService;

    @Mock
    private ProjectPolicyService projectPolicyService;

    @Mock
    private ContentService contentService;

    @Mock
    private EhrBaseService ehrBaseService;

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

    private final String Q1 = "Select c0 as test from EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.report.v1]";
    private final String Q2 = "SELECT c0 as GECCO_Personendaten " +
            "FROM EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.registereintrag.v1] contains CLUSTER c1[openEHR-EHR-CLUSTER.person_birth_data_iso.v0] " +
            "WHERE (c0/archetype_details/template_id/value = 'GECCO_Personendaten'";

    private final String Q3 = "SELECT  c0 as GECCO_Personendaten " +
            "FROM EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.registereintrag.v1] contains CLUSTER c1[openEHR-EHR-CLUSTER.person_birth_data_iso.v0] " +
            "WHERE (c0/archetype_details/template_id/value = 'GECCO_Personendaten' and c1/items[at0001]/value/value = $Geburtsdatum)";

    private final String Q4_SELECT_HEIGHT = "SELECT o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude as Gr__e_L_nge__magnitude " +
            "FROM EHR e  contains COMPOSITION c1[openEHR-EHR-COMPOSITION.registereintrag.v1] n  contains OBSERVATION o0[openEHR-EHR-OBSERVATION.height.v2] " +
            "WHERE  (c1/archetype_details/template_id/value = 'Körpergröße' and o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude = $height)";

    private final String NAME1 = "AQL query name 1";
    private final String NAME2 = "AQL query name 2";
    private final String NAME3 = "Geburtsdatum";

    private final String NAME4 = "Height";

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

    @Test(expected = BadRequestException.class)
    public void shouldCorrectlyValidateCohortEmptyParameters() {
        CohortAqlDto cohortAqlDto1 = CohortAqlDto.builder().id(1L).name(NAME3).query(Q3).build();

        CohortGroupDto childGroup = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto1).build();

        CohortGroupDto andGroupCohort =
                CohortGroupDto.builder()
                        .type(Type.GROUP)
                        .operator(Operator.AND)
                        .children(List.of(childGroup))
                        .build();
        cohortService.getCohortGroupSize(andGroupCohort, approvedUser.getUserId(), false);
        Mockito.verifyNoInteractions(cohortExecutor);
    }

    @Test
    public void shouldCorrectlyValidateCohortMissingParameters() {
        CohortAqlDto cohortAqlDto1 = CohortAqlDto.builder().id(1L).name(NAME3).query(Q3).build();
        CohortAqlDto cohortAqlDto2 = CohortAqlDto.builder().id(2L).name(NAME4).query(Q4_SELECT_HEIGHT).build();

        CohortGroupDto first = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto1).build();
        CohortGroupDto second = CohortGroupDto.builder()
                .type(Type.AQL)
                .query(cohortAqlDto2)
                .parameters(Map.of("height", 160))
                .build();

        CohortGroupDto mainGroup =
                CohortGroupDto.builder()
                        .type(Type.GROUP)
                        .operator(Operator.AND)
                        .children(List.of(first, second))
                        .build();
        try {
            cohortService.getCohortGroupSize(mainGroup, approvedUser.getUserId(), false);
        } catch (BadRequestException be) {
            Assert.assertEquals("The query is invalid. The value of at least one criterion is missing.", be.getMessage());
        }
        Mockito.verifyNoInteractions(cohortExecutor);
    }

    @Test
    public void shouldCorrectlyValidateMissingCohorts() {
        CohortGroupDto mainGroup =
                CohortGroupDto.builder()
                        .type(Type.GROUP)
                        .operator(Operator.AND)
                        .build();
        try {
            cohortService.getCohortGroupSize(mainGroup, approvedUser.getUserId(), false);
        } catch (BadRequestException be) {
            Assert.assertEquals("The query is invalid. Please select at least one criterion.", be.getMessage());
        }
        Mockito.verifyNoInteractions(cohortExecutor);
    }
    @Test
    public void shouldCorrectlyValidateMissingCohortQuery() {
        CohortGroupDto mainGroup =
                CohortGroupDto.builder()
                        .type(Type.AQL)
                        .operator(Operator.OR)
                        .build();
        try {
            cohortService.getCohortGroupSize(mainGroup, approvedUser.getUserId(), false);
        } catch (BadRequestException be) {
            Assert.assertEquals(ExceptionsTemplate.INVALID_COHORT_GROUP_AQL_MISSING, be.getMessage());
        }
        Mockito.verifyNoInteractions(cohortExecutor);
    }

    @Test(expected = PrivacyException.class)
    public void shouldHandlePrivacyPolicyWhenGetCohortSize() {

        CohortAqlDto cohortAqlDto1 = CohortAqlDto.builder().id(1L).name(NAME1).query(Q1).build();

        CohortGroupDto childGroup = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto1).build();

        CohortGroupDto andGroupCohort =
                CohortGroupDto.builder()
                        .type(Type.GROUP)
                        .operator(Operator.AND)
                        .children(List.of(childGroup))
                        .build();
        when(privacyProperties.getMinHits()).thenReturn(5);
        cohortService.getCohortGroupSize(andGroupCohort, approvedUser.getUserId(), false);
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

    @Test
    public void toCohortTest() {
        CohortAqlDto cohortAqlDto = CohortAqlDto.builder().id(1L).name(NAME1).query(Q1).build();
        CohortGroupDto groupDto = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto).build();
        CohortDto cohortDto = CohortDto.builder()
                .name("cohort name")
                .description("test to cohort method")
                .cohortGroup(groupDto)
                .build();
        Cohort result = cohortService.toCohort(cohortDto);
        Assert.assertNotNull(result);
        Assert.assertEquals("cohort name", result.getName());
        Assert.assertEquals("test to cohort method", result.getDescription());
        CohortGroup group = result.getCohortGroup();
        Assert.assertNotNull(group);
        Assert.assertEquals(Type.AQL, group.getType());
        Assert.assertNotNull(group.getQuery());
        Assert.assertEquals(NAME1, group.getQuery().getName());
        Assert.assertEquals(Q1, group.getQuery().getQuery());
    }

    @Test
    public void getRoundedSizeTest() {
        Assert.assertEquals(40, cohortService.getRoundedSize(38));
        Assert.assertEquals(30, cohortService.getRoundedSize(33));
    }

    @Test
    public void getSizePerTemplatesTest() {
        String query = "SELECT c0 as GECCO_Personendaten " +
                "FROM EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.registereintrag.v1] " +
                "contains CLUSTER c1[openEHR-EHR-CLUSTER.person_birth_data_iso.v0] " +
                "WHERE  (c0/archetype_details/template_id/value = 'GECCO_Personendaten' and c1/items[at0001]/value/value > $Geburtsdatum";
        when(aqlService.existsById(99L)).thenReturn(true);
        CohortDto cohortDto = CohortDto.builder()
                .cohortGroup(CohortGroupDto.builder()
                        .operator(Operator.AND)
                        .type(Type.AQL)
                        .query(CohortAqlDto.builder()
                                .name("Geburtsdatum")
                                .id(99L)
                                .query(query)
                                .build())
                        .parameters(Map.of("Geburtsdatum", "1925-01-04"))
                        .build())
                .build();
        TemplateSizeRequestDto requestDto = TemplateSizeRequestDto.builder()
                .templateIds(Arrays.asList("Alter"))
                .cohortDto(cohortDto)
                .build();
        when(cohortExecutor.execute(any(Cohort.class), Mockito.eq(false)))
                .thenReturn(Set.of("1", "2", "3", "4", "5"));
        org.ehrbase.aql.dto.AqlDto aqlDto  = new AqlToDtoParser().parse(query);
        when(templateService.createSelectCompositionQuery(Mockito.eq("Alter"))).thenReturn(aqlDto);
        when(ehrBaseService.retrieveEligiblePatientIds(Mockito.any(String.class))).thenReturn(Set.of("id1", "id2", "id3"));
        cohortService.getSizePerTemplates(approvedUser.getUserId(), requestDto);
        verify(cohortExecutor, times(1)).execute(any(), anyBoolean());
    }

    @Test
    public void getCohortGroupSizeWithDistributionTest() {
        CohortAqlDto cohortAqlDto = CohortAqlDto.builder().id(1L).name(NAME1).query(Q1).build();
        CohortGroupDto groupDto = CohortGroupDto.builder().type(Type.AQL).query(cohortAqlDto).build();
        Mockito.when(contentService.getClinics(approvedUser.getUserId())).thenReturn(Arrays.asList("clinic one"));
        QueryResponseData responseData1 = new QueryResponseData();
        responseData1.setRows(  List.of(
                new ArrayList<>(List.of("ehr-id-1", Map.of("_type", "OBSERVATION", "uuid", "12"))),
                new ArrayList<>(List.of("ehr-id-2", Map.of("_type", "OBSERVATION", "uuid", "123")))));
        String sizePerHopitalQuery = String.format(CohortService.GET_PATIENTS_PER_CLINIC, "clinic one", "'test1','test2'");
        when(ehrBaseService.executePlainQuery(Mockito.eq(sizePerHopitalQuery))).thenReturn(responseData1);
        QueryResponseData responseData2 = new QueryResponseData();
        responseData2.setRows( List.of(new ArrayList<>(List.of(10))));
        for (int age = 0; age < 122; age += 10) {
            String sizePerAgeQuery = String.format(CohortService.GET_PATIENTS_PER_AGE_INTERVAL, age, age+10, "'test1','test2'");
            when(ehrBaseService.executePlainQuery(Mockito.eq(sizePerAgeQuery))).thenReturn(responseData2);
        }
        CohortSizeDto cohortSizeDto = cohortService.getCohortGroupSizeWithDistribution(groupDto, approvedUser.getUserId(), false);
        Assert.assertNotNull(cohortSizeDto);
        Assert.assertEquals(1, cohortSizeDto.getHospitals().size());
    }

    @Test
    public void executeCohortTest() {
        cohortService.executeCohort(Cohort.builder().id(2L).build(), false);
        verify(cohortExecutor, times(1)).execute(any(), anyBoolean());
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
