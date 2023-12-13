package de.vitagroup.num.service.policy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.ehrbase.aql.dto.condition.MatchesOperatorDto;
import org.ehrbase.aql.dto.condition.SimpleValue;
import org.ehrbase.aql.dto.condition.Value;
import org.ehrbase.aql.dto.containment.ContainmentExpresionDto;
import org.ehrbase.aql.dto.containment.ContainmentLogicalOperator;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.MatchesCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.WhereCondition;
import org.ehrbase.openehr.sdk.aql.dto.containment.Containment;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperatorSymbol;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.MatchesOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Operand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;

/** Defines a certain project policy to be applied to an aql query */
public abstract class Policy {

  private static final String COMPOSITION_ARCHETYPE_ID = "COMPOSITION";

  public abstract boolean apply(AqlQuery aql);

  protected void restrictAqlWithCompositionAttribute(AqlQuery aql, String attributePath, List<Primitive> attributeValues) {

    List<IdentifiedPath> whereClauseSelectFields = new LinkedList<>();

    //List<SelectFieldDto> whereClauseSelectFields = new LinkedList<>();

    ContainmentExpresionDto contains = aql.ge;
    int nextContainmentId = findNextContainmentId(contains);

    if (contains != null) {
      List<Integer> compositions = findCompositions(contains);

      if (CollectionUtils.isNotEmpty(compositions)) {
        compositions.forEach(
            id -> {
//              SelectFieldDto selectFieldDto = new SelectFieldDto();
//              selectFieldDto.setAqlPath(attributePath);
//              selectFieldDto.setContainmentId(id);

              IdentifiedPath selectFieldDto = new IdentifiedPath();
              selectFieldDto.setPath(AqlObjectPath.parse(attributePath));

              whereClauseSelectFields.add(selectFieldDto);
            });

      } else {
        extendContainsClause(aql, whereClauseSelectFields, contains, nextContainmentId, attributePath);
      }
    } else {
      createContainsClause(aql, whereClauseSelectFields, nextContainmentId, attributePath);
    }
    extendWhereClause(aql, whereClauseSelectFields, attributeValues);
  }

  protected void createContainsClause(AqlQuery aql, List<IdentifiedPath> whereClauseSelectFields,
                                      int nextContainmentId,
                                      String path) {
    IdentifiedPath selectField = new IdentifiedPath();
    selectField.setPath(AqlObjectPath.parse(path));
    whereClauseSelectFields.add(selectField);

//    SelectFieldDto select = new SelectFieldDto();
//    select.setAqlPath(path);
//    select.setContainmentId(nextContainmentId);
//    whereClauseSelectFields.add(select);

    ContainmentClassExpression ct = new ContainmentClassExpression();
    //ct.setContains();
    //ContainmentDto composition = new ContainmentDto();
    //composition.setId(nextContainmentId);
    //composition.setArchetypeId(COMPOSITION_ARCHETYPE_ID);
    //aql.setFrom(selectField);
  }

  protected void extendWhereClause(AqlQuery aql, List<IdentifiedPath> selects, List<Primitive> values) {
    List<MatchesCondition> matchesOperatorDtos = new LinkedList<>();

    selects.forEach(
            selectFieldDto -> {
              MatchesCondition matches = new MatchesCondition();
              matches.setStatement(selectFieldDto);
              matches.setValues(values.stream().map(v -> v.get));
              matchesOperatorDtos.add(matches);
            });

    LogicalOperatorCondition newWhere = new LogicalOperatorCondition();
    newWhere.setValues(new ArrayList<>());
    WhereCondition where = aql.getWhere();

    if (where != null) {
      newWhere.setSymbol(LogicalOperatorCondition.ConditionLogicalOperatorSymbol.AND);
      newWhere.getValues().add(where);
    }

    if (CollectionUtils.isNotEmpty(matchesOperatorDtos) && matchesOperatorDtos.size() > 1) {
      newWhere.setSymbol(LogicalOperatorCondition.ConditionLogicalOperatorSymbol.AND);
    }

    matchesOperatorDtos.forEach(
            matchesOperatorDto -> newWhere.getValues().add(matchesOperatorDto));

    aql.setWhere(newWhere);
  }

  protected void extendContainsClause(AqlQuery aql, List<IdentifiedPath> whereClauseSelectFields,
          Containment contains, int nextContainmentId,  String path) {
    IdentifiedPath select = new IdentifiedPath();
    select.setPath(AqlObjectPath.parse(path));
    //select.setContainmentId(nextContainmentId);
    whereClauseSelectFields.add(select);

    ContainmentSetOperator newContains = new ContainmentSetOperator();
    newContains.setValues(new ArrayList<>());


//    ContainmentDto composition = new ContainmentDto();
//    composition.setId(nextContainmentId);
//    composition.getContainment().setArchetypeId(COMPOSITION_ARCHETYPE_ID);

    newContains.setSymbol(ContainmentSetOperatorSymbol.AND);
    //newContains.getValues().add(composition);
    newContains.getValues().add(contains);

    aql.setFrom(newContains);
  }

  protected List<Primitive> toSimpleValueList(Collection<String> list) {
    return list.stream()
        .map()
        .collect(Collectors.toList());
  }

  protected List<Integer> findCompositions(Containment dto) {
    if (dto == null) {
      return null;
    }

    List<Integer> compositions = new LinkedList<>();

    Queue<Containment> queue = new ArrayDeque<>();
    queue.add(dto);

    while (!queue.isEmpty()) {
      Containment current = queue.remove();

      if (current instanceof ContainmentSetOperator containmentLogicalOperator) {

        queue.addAll(containmentLogicalOperator.getValues());

      } //else if (current instanceof ContainmentDto containmentDto) {

//        if (containmentDto.getContainment().getArchetypeId().contains(COMPOSITION_ARCHETYPE_ID)) {
//          compositions.add(containmentDto.getId());
//        }
//
//        if (containmentDto.getContains() != null) {
//          queue.add(containmentDto.getContains());
//        }
      //}
    }
    return compositions;
  }

  protected Integer findNextContainmentId(Containment dto) {

    if (dto == null) {
      return 1;
    }

    Queue<Containment> queue = new ArrayDeque<>();
    queue.add(dto);

    int nextId = 0;

    while (!queue.isEmpty()) {
      Containment current = queue.remove();

      if (current instanceof ContainmentSetOperator containmentLogicalOperator) {

        queue.addAll(containmentLogicalOperator.getValues());

      } //else if (current instanceof ContainmentDto containmentDto) {

//        if (containmentDto.getId() > nextId) {
//          nextId = containmentDto.getId();
//        }
//
//        if (containmentDto.getContains() != null) {
//          queue.add(containmentDto.getContains());
//        }
      //}
    }
    return nextId + 1;
  }
}
