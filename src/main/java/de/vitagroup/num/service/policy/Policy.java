package de.vitagroup.num.service.policy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.ConditionDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorSymbol;
import org.ehrbase.aql.dto.condition.MatchesOperatorDto;
import org.ehrbase.aql.dto.condition.SimpleValue;
import org.ehrbase.aql.dto.condition.Value;
import org.ehrbase.aql.dto.containment.ContainmentDto;
import org.ehrbase.aql.dto.containment.ContainmentExpresionDto;
import org.ehrbase.aql.dto.containment.ContainmentLogicalOperator;
import org.ehrbase.aql.dto.containment.ContainmentLogicalOperatorSymbol;
import org.ehrbase.aql.dto.select.SelectFieldDto;

/** Defines a certain project policy to be applied to an aql query */
public abstract class Policy {

  private static final String COMPOSITION_ARCHETYPE_ID = "COMPOSITION";

  public abstract boolean apply(AqlDto aql);

  protected void restrictAqlWithCompositionAttribute(
      AqlDto aql, String attributePath, List<Value> attributeValues) {

    List<SelectFieldDto> whereClauseSelectFields = new LinkedList<>();

    ContainmentExpresionDto contains = aql.getContains();
    int nextContainmentId = findNextContainmentId(contains);

    if (contains != null) {
      List<Integer> compositions = findCompositions(contains);

      if (CollectionUtils.isNotEmpty(compositions)) {
        compositions.forEach(
            id -> {
              SelectFieldDto selectFieldDto = new SelectFieldDto();
              selectFieldDto.setAqlPath(attributePath);
              selectFieldDto.setContainmentId(id);
              whereClauseSelectFields.add(selectFieldDto);
            });

      } else {
        extendContainsClause(
            aql, whereClauseSelectFields, contains, nextContainmentId, attributePath);
      }
    } else {
      createContainsClause(aql, whereClauseSelectFields, nextContainmentId, attributePath);
    }
    extendWhereClause(aql, whereClauseSelectFields, attributeValues);
  }

  protected void createContainsClause(
      AqlDto aql,
      List<SelectFieldDto> whereClauseSelectFields,
      int nextContainmentId,
      String path) {

    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(path);
    select.setContainmentId(nextContainmentId);
    whereClauseSelectFields.add(select);

    ContainmentDto composition = new ContainmentDto();
    composition.setId(nextContainmentId);
    composition.setArchetypeId(COMPOSITION_ARCHETYPE_ID);
    aql.setContains(composition);
  }

  protected void extendWhereClause(AqlDto aql, List<SelectFieldDto> selects, List<Value> values) {
    List<MatchesOperatorDto> matchesOperatorDtos = new LinkedList<>();

    selects.forEach(
        selectFieldDto -> {
          MatchesOperatorDto matches = new MatchesOperatorDto();
          matches.setStatement(selectFieldDto);
          matches.setValues(values);
          matchesOperatorDtos.add(matches);
        });

    ConditionLogicalOperatorDto newWhere = new ConditionLogicalOperatorDto();
    newWhere.setValues(new ArrayList<>());
    ConditionDto where = aql.getWhere();

    if (where != null) {
      newWhere.setSymbol(ConditionLogicalOperatorSymbol.AND);
      newWhere.getValues().add(where);
    }

    if (CollectionUtils.isNotEmpty(matchesOperatorDtos) && matchesOperatorDtos.size() > 1) {
      newWhere.setSymbol(ConditionLogicalOperatorSymbol.AND);
    }

    matchesOperatorDtos.forEach(
        matchesOperatorDto -> newWhere.getValues().add(matchesOperatorDto));

    aql.setWhere(newWhere);
  }

  protected void extendContainsClause(
      AqlDto aql,
      List<SelectFieldDto> whereClauseSelectFields,
      ContainmentExpresionDto contains,
      int nextContainmentId,
      String path) {
    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(path);
    select.setContainmentId(nextContainmentId);
    whereClauseSelectFields.add(select);

    ContainmentLogicalOperator newContains = new ContainmentLogicalOperator();
    newContains.setValues(new ArrayList<>());

    ContainmentDto composition = new ContainmentDto();
    composition.setId(nextContainmentId);
    composition.setArchetypeId(COMPOSITION_ARCHETYPE_ID);

    newContains.setSymbol(ContainmentLogicalOperatorSymbol.AND);
    newContains.getValues().add(composition);
    newContains.getValues().add(contains);

    aql.setContains(newContains);
  }

  protected List<Value> toSimpleValueList(Collection<String> list) {
    return list.stream()
        .map(
            s -> new SimpleValue(s))
        .collect(Collectors.toList());
  }

  protected List<Integer> findCompositions(ContainmentExpresionDto dto) {
    if (dto == null) {
      return null;
    }

    List<Integer> compositions = new LinkedList<>();

    Queue<ContainmentExpresionDto> queue = new ArrayDeque<>();
    queue.add(dto);

    while (!queue.isEmpty()) {
      ContainmentExpresionDto current = queue.remove();

      if (current instanceof ContainmentLogicalOperator) {

        ContainmentLogicalOperator containmentLogicalOperator =
            (ContainmentLogicalOperator) current;

        queue.addAll(containmentLogicalOperator.getValues());

      } else if (current instanceof ContainmentDto) {

        ContainmentDto containmentDto = (ContainmentDto) current;

        if (containmentDto.getArchetypeId().contains(COMPOSITION_ARCHETYPE_ID)) {
          compositions.add(containmentDto.getId());
        }

        if (containmentDto.getContains() != null) {
          queue.add(containmentDto.getContains());
        }
      }
    }
    return compositions;
  }

  protected Integer findNextContainmentId(ContainmentExpresionDto dto) {

    if (dto == null) {
      return 1;
    }

    Queue<ContainmentExpresionDto> queue = new ArrayDeque<>();
    queue.add(dto);

    int nextId = 0;

    while (!queue.isEmpty()) {
      ContainmentExpresionDto current = queue.remove();

      if (current instanceof ContainmentLogicalOperator) {

        ContainmentLogicalOperator containmentLogicalOperator =
            (ContainmentLogicalOperator) current;

        queue.addAll(containmentLogicalOperator.getValues());

      } else if (current instanceof ContainmentDto) {

        ContainmentDto containmentDto = (ContainmentDto) current;

        if (containmentDto.getId() > nextId) {
          nextId = containmentDto.getId();
        }

        if (containmentDto.getContains() != null) {
          queue.add(containmentDto.getContains());
        }
      }
    }
    return nextId + 1;
  }
}
