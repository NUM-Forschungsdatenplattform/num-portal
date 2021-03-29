/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.service.exception.IllegalArgumentException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class SetOperationsService {

  public Set<String> apply(Operator operator, List<Set<String>> sets, Set<String> all) {
    switch (operator) {
      case AND:
        return intersection(sets);
      case OR:
        return union(sets);
      case NOT:
        return exclude(all, sets.get(0));
      default:
        return SetUtils.emptySet();
    }
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
      throw new IllegalArgumentException("Relative complement requires two valid sets");
    }

    Set<String> fromCopy = new HashSet<>(from);
    fromCopy.removeAll(excludeSet);
    return fromCopy;
  }
}
