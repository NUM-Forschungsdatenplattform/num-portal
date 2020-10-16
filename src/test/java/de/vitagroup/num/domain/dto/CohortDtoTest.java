package de.vitagroup.num.domain.dto;

import de.vitagroup.num.converter.CohortConverter;
import de.vitagroup.num.domain.*;
import de.vitagroup.num.service.PhenotypeService;
import de.vitagroup.num.service.StudyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CohortDtoTest {

    @Mock
    private PhenotypeService phenotypeService;

    @Mock
    private StudyService studyService;

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private CohortConverter converter;

    @Before
    public void setup() {
        Study study = Study.builder().id(1L).name("Study name").description("Study description").build();

        Aql aql1 = Aql.builder().id(1L).name("AQL query name 1 ").query("SELECT A1 ... FROM E1... WHERE ...").build();
        Aql aql2 = Aql.builder().id(2L).name("AQL query name 2").query("SELECT A2 ... FROM E2... WHERE ...").build();

        AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
        AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

        Phenotype phenotype1 = Phenotype.builder().id(1L).name("Phenotype name").description("Phenotype description").query(aqlExpression1).build();
        Phenotype phenotype2 = Phenotype.builder().id(2L).name("Phenotype name").description("Phenotype description").query(aqlExpression2).build();

        when(studyService.getStudyById(any())).thenReturn(Optional.of(study));
        when(phenotypeService.getPhenotypeById(1L)).thenReturn(Optional.of(phenotype1));
        when(phenotypeService.getPhenotypeById(2L)).thenReturn(Optional.of(phenotype2));
    }

    @Test
    public void shouldCorrectlyConvertCohortToCohortDto() {
        Study study = Study.builder().id(1L).name("Study name").description("Study description").build();

        Aql aql1 = Aql.builder().id(1L).name("AQL query name 1 ").query("SELECT A1 ... FROM E1... WHERE ...").build();
        Aql aql2 = Aql.builder().id(2L).name("AQL query name 2").query("SELECT A2 ... FROM E2... WHERE ...").build();

        AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
        AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

        Phenotype phenotype1 = Phenotype.builder().id(1L).name("Phenotype name").description("Phenotype description").query(aqlExpression1).build();
        Phenotype phenotype2 = Phenotype.builder().id(2L).name("Phenotype name").description("Phenotype description").query(aqlExpression2).build();

        CohortGroup first = CohortGroup.builder().type(Type.PHENOTYPE).phenotype(phenotype1).build();
        CohortGroup second = CohortGroup.builder().type(Type.PHENOTYPE).phenotype(phenotype2).build();

        CohortGroup andCohort = CohortGroup.builder().type(Type.GROUP).operator(Operator.AND).children(Set.of(first, second)).build();

        Cohort cohort = Cohort.builder().name("Cohort name").study(study).cohortGroup(andCohort).build();
        CohortDto cohortDto = converter.convertToDto(cohort);

        assertThat(cohortDto.getName(), notNullValue());
        assertThat(cohortDto.getName(), is("Cohort name"));

        assertThat(cohortDto.getStudyId(), is(study.getId()));

        assertThat(cohortDto.getCohortGroupDto(), notNullValue());
        assertThat(cohortDto.getCohortGroupDto().getType(), is(Type.GROUP));
        assertThat(cohortDto.getCohortGroupDto().getOperator(), is(Operator.AND));
        assertThat(cohortDto.getCohortGroupDto().getChildren().size(), is(2));

        assertThat(cohortDto.getCohortGroupDto().getChildren().get(0).getType(), is(Type.PHENOTYPE));
        assertThat(cohortDto.getCohortGroupDto().getChildren().get(1).getType(), is(Type.PHENOTYPE));

        assertThat(cohortDto.getCohortGroupDto().getChildren().stream().anyMatch(c -> c.getPhenotypeId() == 1), is(true));
        assertThat(cohortDto.getCohortGroupDto().getChildren().stream().anyMatch(c -> c.getPhenotypeId() == 2), is(true));
    }

    @Test
    public void shouldCorrectlyConvertCohortDtoToCohort() {
        CohortGroupDto first = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).build();
        CohortGroupDto second = CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(2L).build();

        CohortGroupDto andCohort = CohortGroupDto.builder().type(Type.GROUP).operator(Operator.AND).children(List.of(first, second)).build();

        CohortDto cohortDto = CohortDto.builder().name("Cohort name").studyId(1L).cohortGroupDto(andCohort).build();
        Cohort cohort = converter.convertToEntity(cohortDto);

        assertThat(cohort, notNullValue());
        assertThat(cohort.getStudy(), notNullValue());
        assertThat(cohort.getStudy().getId(), is(1L));
        assertThat(cohort.getStudy().getName(), is("Study name"));
        assertThat(cohort.getCohortGroup().getOperator(), is(Operator.AND));
        assertThat(cohort.getCohortGroup().getType(), is(Type.GROUP));
        assertThat(cohort.getCohortGroup().getChildren().size(), is(2));

        assertThat(cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getPhenotype().getId() == 1), is(true));
        assertThat(cohort.getCohortGroup().getChildren().stream().anyMatch(c -> c.getPhenotype().getId() == 2), is(true));

        assertThat(cohort.getCohortGroup().getChildren().stream().allMatch(c -> c.getPhenotype().getQuery() instanceof AqlExpression), is(true));

    }


}
