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
public class SetOperations {

    public Set<Integer> apply(Operator operator, List<Set<Integer>> sets, Set<Integer> all){
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

    public Set<Integer> intersection(List<Set<Integer>> listOfSets) {
        HashSet<Integer> intersection = new HashSet<>();
        listOfSets.stream().filter(Objects::nonNull).forEach(intersection::addAll);

        listOfSets.stream().filter(Objects::nonNull).forEach(intersection::retainAll);
        return intersection;
    }

    public Set<Integer> union(List<Set<Integer>> listOfSets) {
        HashSet<Integer> union = new HashSet<>();
        listOfSets.stream().filter(Objects::nonNull).forEach(union::addAll);
        return union;
    }

    public Set<Integer> exclude(Set<Integer> from, Set<Integer> excludeSet) {

        if(CollectionUtils.isEmpty(from) || CollectionUtils.isEmpty(excludeSet)){
            throw new IllegalArgumentException("Relative complement requires two valid sets");
        }

        Set<Integer> fromCopy = new HashSet<>(from);
        fromCopy.removeAll(excludeSet);
        return fromCopy;
    }
}
