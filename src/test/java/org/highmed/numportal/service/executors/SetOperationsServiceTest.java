package org.highmed.numportal.service.executors;

import org.apache.commons.collections4.SetUtils;
import org.highmed.numportal.service.executors.SetOperationsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.numportal.domain.model.Operator;
import org.highmed.numportal.service.exception.IllegalArgumentException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(MockitoJUnitRunner.class)
public class SetOperationsServiceTest {

  @InjectMocks
  private SetOperationsService setOperations;

  @Test
  public void shouldCorrectlyComputeSingleSetIntersection() {
    Set<String> result = setOperations.intersection(List.of(Set.of("1", "2", "3", "8", "9", "5")));

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "2", "3", "8", "9", "5")), is(true));
  }

  @Test
  public void shouldCorrectlyComputeSetsIntersection() {
    Set<String> result =
        setOperations.intersection(
            List.of(
                Set.of("1", "2", "3", "8", "9", "5"),
                Set.of("3", "4", "5", "1"),
                Set.of("3", "5", "6", "1"),
                Set.of("1", "3", "5", "6")));

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "3", "5")), is(true));
  }

  @Test
  public void shouldCorrectlyComputeSingleSetUnion() {
    Set<String> result = setOperations.intersection(List.of(Set.of("1", "2", "3", "8", "9", "5")));

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "2", "3", "8", "9", "5")), is(true));
  }

  @Test
  public void shouldCorrectlyComputeSetsUnion() {
    Set<String> result =
        setOperations.union(
            List.of(
                Set.of("1", "2", "3", "8", "9"),
                Set.of("1", "5"),
                Set.of("1", "3", "5", "6", "7", "10", "11"),
                Set.of("4")));

    assertThat(result, notNullValue());
    assertThat(
        result.equals(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")), is(true));
  }

  @Test
  public void shouldCorrectlyComputeSetsRelativeComplement() {
    Set<String> result = setOperations.exclude(Set.of("1", "2", "3", "8", "9"), Set.of("1", "5"));

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("2", "3", "8", "9")), is(true));
  }

  @Test
  public void shouldHandleUnionNullValues() {
    List<Set<String>> sets = new LinkedList<>();

    sets.add(null);
    sets.add(Set.of("1", "2", "3", "8", "9"));
    sets.add(Set.of("1", "5"));
    sets.add(Set.of("1", "3", "5", "6", "7", "10", "11"));
    sets.add(Set.of("4"));

    Set<String> result = setOperations.union(sets);

    assertThat(result, notNullValue());
    assertThat(
        result.equals(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")), is(true));
  }

  @Test
  public void shouldHandleIntersectionNullValues() {
    List<Set<String>> sets = new LinkedList<>();

    sets.add(null);
    sets.add(Set.of("1", "2", "3", "8", "9", "5"));
    sets.add(Set.of("3", "4", "5", "1"));
    sets.add(Set.of("3", "5", "6", "1"));
    sets.add(Set.of("3", "5", "6", "1"));

    Set<String> result = setOperations.intersection(sets);
    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "3", "5")), is(true));
  }

  @Test
  public void shouldHandleIntersectionEmptySets() {
    Set<String> result =
        setOperations.intersection(
            List.of(
                SetUtils.emptySet(),
                Set.of("1", "2", "3", "8", "9", "5"),
                Set.of("3", "4", "5", "1"),
                Set.of("3", "5", "6", "1"),
                Set.of("3", "5", "6", "1")));

    assertThat(result, notNullValue());
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void shouldHandleUnionEmptySets() {
    Set<String> result =
        setOperations.union(
            List.of(
                SetUtils.emptySet(),
                Set.of("1", "2", "3", "8", "9"),
                Set.of("1", "5"),
                Set.of("1", "3", "5", "6", "7", "10"),
                Set.of("4")));

    assertThat(result, notNullValue());
    assertThat(result.equals(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")), is(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldHandleRelativeComplementNullValues() {
    setOperations.exclude(null, Set.of("1", "5"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldHandleEmptyExcludeValues() {
    setOperations.exclude(Set.of("1", "5"), Collections.emptySet());
  }

  @Test
  public void shouldCorrectlyApplyAndOperation() {

    Set<String> results =
        setOperations.apply(
            Operator.AND, List.of(Set.of("1", "2", "3"), Set.of("1", "2", "4")), Set.of());

    assertThat(results, notNullValue());
    assertThat(results.equals(Set.of("1", "2")), is(true));
  }

  @Test
  public void shouldCorrectlyApplyOrOperation() {
    Set<String> results =
        setOperations.apply(
            Operator.OR,
            List.of(Set.of("1abc", "2abc", "3abc"), Set.of("4abc", "5abc", "6abc", "7abc")),
            Set.of());

    assertThat(results, notNullValue());
    assertThat(
        results.equals(Set.of("1abc", "2abc", "3abc", "4abc", "5abc", "6abc", "7abc")), is(true));
  }

  @Test
  public void shouldCorrectlyApplyNotOperation() {
    Set<String> results =
        setOperations.apply(Operator.NOT, List.of(Set.of("1abc", "2abc", "3abc")), Set.of("4abc"));

    assertThat(results, notNullValue());
    assertThat(results.equals(Set.of("4abc")), is(true));
  }
}
