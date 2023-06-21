package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.service.exception.IllegalArgumentException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.RELATIVE_COMPLEMENT_REQUIRES_TWO_VALID_SETS;

@Service
public class SetOperationsService {

  public Set<String> apply(Operator operator, List<Set<String>> sets, Set<String> all) {
    return switch (operator) {
      case AND -> intersection(sets);
      case OR -> union(sets);
      case NOT -> exclude(all, sets.get(0));
    };
  }

  public Set<String> intersection(List<Set<String>> listOfSets) {
    HashSet<String> intersection = new HashSet<>();
    listOfSets.stream().filter(Objects::nonNull).forEach(intersection::addAll);

    listOfSets.stream().filter(Objects::nonNull).forEach(intersection::retainAll);
    return intersection;
  }

  public Set<String> union(List<Set<String>> listOfSets) {
    HashSet<String> union = new HashSet<>();
    listOfSets.stream().filter(Objects::nonNull).forEach(union::addAll);
    return union;
  }

  public Set<String> exclude(Set<String> from, Set<String> excludeSet) {

    if (CollectionUtils.isEmpty(from) || CollectionUtils.isEmpty(excludeSet)) {
      throw new IllegalArgumentException(SetOperationsService.class, RELATIVE_COMPLEMENT_REQUIRES_TWO_VALID_SETS);
    }

    Set<String> fromCopy = new HashSet<>(from);
    fromCopy.removeAll(excludeSet);
    return fromCopy;
  }
}
