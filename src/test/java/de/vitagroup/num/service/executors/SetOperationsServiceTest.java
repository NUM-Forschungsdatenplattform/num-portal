package de.vitagroup.num.service.executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import de.vitagroup.num.service.exception.IllegalArgumentException;
import org.apache.commons.collections4.SetUtils;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SetOperationsServiceTest {

  private final SetOperationsService setOperations = new SetOperationsService();

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
}
