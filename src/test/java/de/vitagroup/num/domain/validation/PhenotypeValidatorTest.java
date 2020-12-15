package de.vitagroup.num.domain.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import de.vitagroup.num.domain.*;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.Set;

public class PhenotypeValidatorTest {

  private Validator validator;

  private final String AQL_NAME = "AQL query name";
  private final String AQL_QUERY = "SELECT A ... FROM E ... WHERE ...";

  @Before
  public void setup() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void supportsSingleAqlQuery() {
    Aql aql = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    AqlExpression aqlExpression = AqlExpression.builder().aql(aql).build();
    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("Single AQL valid phenotype")
            .query(aqlExpression)
            .build();

    Set<ConstraintViolation<Phenotype>> violations = validator.validate(phenotype);
    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  public void supportsSimpleAndOperation() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.AND)
            .children(Arrays.asList(aqlExpression1, aqlExpression2))
            .build();

    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("Two AQLs valid phenotype")
            .query(query)
            .build();

    Set<ConstraintViolation<Phenotype>> violations = validator.validate(phenotype);
    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  public void supportsSimpleOrOperation() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.OR)
            .children(Arrays.asList(aqlExpression1, aqlExpression2))
            .build();

    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("Two AQLs valid phenotype")
            .query(query)
            .build();

    Set<ConstraintViolation<Phenotype>> violations = validator.validate(phenotype);
    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  public void supportsSimpleNotOperation() {
    Aql aql = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    AqlExpression aqlExpression = AqlExpression.builder().aql(aql).build();

    GroupExpression notAql =
        GroupExpression.builder()
            .operator(Operator.NOT)
            .children(Arrays.asList(aqlExpression))
            .build();

    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("One AQL valid phenotype")
            .query(notAql)
            .build();

    Set<ConstraintViolation<Phenotype>> violations = validator.validate(phenotype);
    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  public void correctlyValidatesNotOperation() {
    Aql aql1 = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    Aql aql2 = Aql.builder().id(2L).name(AQL_NAME).query(AQL_QUERY).build();

    AqlExpression aqlExpression1 = AqlExpression.builder().aql(aql1).build();
    AqlExpression aqlExpression2 = AqlExpression.builder().aql(aql2).build();

    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.NOT)
            .children(Arrays.asList(aqlExpression1, aqlExpression2))
            .build();

    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("NOT operation can be applied to single AQL query")
            .query(query)
            .build();

    Set<ConstraintViolation<Phenotype>> violations = validator.validate(phenotype);
    assertThat(violations.isEmpty(), is(false));
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getMessage(), is("Invalid phenotype definition"));
  }

  @Test
  public void correctlyValidatesOrOperation() {
    Aql aql = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();

    AqlExpression aqlExpression = AqlExpression.builder().aql(aql).build();
    GroupExpression query =
        GroupExpression.builder()
            .operator(Operator.OR)
            .children(Arrays.asList(aqlExpression))
            .build();

    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("OR operation cannot be applied to a single AQL query")
            .query(query)
            .build();

    Set<ConstraintViolation<Phenotype>> violations = validator.validate(phenotype);
    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  public void correctlyValidatesPhenotypeNameAndDescription() {
    Aql aql = Aql.builder().id(1L).name(AQL_NAME).query(AQL_QUERY).build();
    AqlExpression query = AqlExpression.builder().aql(aql).build();
    Phenotype noNameNoDescriptionPhenotype = Phenotype.builder().id(1L).query(query).build();

    Set<ConstraintViolation<Phenotype>> violations =
        validator.validate(noNameNoDescriptionPhenotype);
    assertThat(violations.isEmpty(), is(false));
    assertThat(violations.size(), is(2));

    assertThat(
        violations.stream()
            .anyMatch(v -> v.getMessage().equals("Phenotype description is mandatory")),
        is(true));
    assertThat(
        violations.stream().anyMatch(v -> v.getMessage().equals("Phenotype name is mandatory")),
        is(true));
  }

  @Test
  public void correctlyValidatesMissingAql() {
    AqlExpression query = AqlExpression.builder().build();
    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("Phenotype name")
            .description("Missing aql query")
            .query(query)
            .build();

    Set<ConstraintViolation<Phenotype>> violations = validator.validate(phenotype);
    assertThat(violations.isEmpty(), is(false));
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getMessage(), is("Invalid phenotype definition"));
  }
}
