package de.vitagroup.num.service.policy;

import de.vitagroup.num.service.util.AqlQueryConstants;
import org.apache.commons.collections.CollectionUtils;
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
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/** Defines a certain project policy to be applied to an aql query */
public abstract class Policy {

  public abstract boolean apply(AqlQuery aql);

  protected void restrictAqlWithCompositionAttribute(AqlQuery aql, String attributePath, List<Primitive> attributeValues) {

    List<SelectExpression> whereClauseSelectFields = new LinkedList<>();

    ContainmentClassExpression originalFrom = (ContainmentClassExpression) aql.getFrom();
    Containment contains = originalFrom.getContains();

    int nextContainmentId = findNextContainmentId(contains);

    if (contains != null) {
      List<String> compositions = findCompositionsIdentifier(contains);

      if (CollectionUtils.isNotEmpty(compositions)) {
        compositions.forEach(
            id -> {
              IdentifiedPath selectFieldDto = new IdentifiedPath();
              selectFieldDto.setPath(AqlObjectPath.parse(attributePath));

              ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
              containmentClassExpression.setType(AqlQueryConstants.COMPOSITION_TYPE);
              containmentClassExpression.setIdentifier(id);
              selectFieldDto.setRoot(containmentClassExpression);

              SelectExpression se = new SelectExpression();
              se.setColumnExpression(selectFieldDto);
              whereClauseSelectFields.add(se);
            });

      } else {
        extendContainsClause(aql, whereClauseSelectFields, contains, nextContainmentId, attributePath);
      }
    } else {
      createContainsClause(aql, whereClauseSelectFields, nextContainmentId, attributePath);
    }
    extendWhereClause(aql, whereClauseSelectFields, attributeValues);
  }

  protected void createContainsClause(AqlQuery aql, List<SelectExpression> whereClauseSelectFields, int nextContainmentId, String attrPath) {
    IdentifiedPath selectField = new IdentifiedPath();
    selectField.setPath(AqlObjectPath.parse(attrPath));

    ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
    containmentClassExpression.setType(AqlQueryConstants.COMPOSITION_TYPE);
    containmentClassExpression.setIdentifier("c" + nextContainmentId);
    selectField.setRoot(containmentClassExpression);

    SelectExpression se = new SelectExpression();
    se.setColumnExpression(selectField);
    whereClauseSelectFields.add(se);

    ContainmentClassExpression from = (ContainmentClassExpression) aql.getFrom();
    from.setContains(containmentClassExpression);
  }

  protected void extendWhereClause(AqlQuery aql, List<SelectExpression> selectExpressions, List<Primitive> values) {
    List<WhereCondition> whereConditions = new ArrayList<>();

    selectExpressions.forEach(
            selectFieldDto -> {
              MatchesCondition matches = new MatchesCondition();
              matches.setStatement((IdentifiedPath) selectFieldDto.getColumnExpression());
              List<MatchesOperand> operands = values.stream()
                      .map(v -> new StringPrimitive(v.getValue().toString()))
                      .collect(Collectors.toList());
              matches.setValues(operands);
              whereConditions.add(matches);
            });

    LogicalOperatorCondition newWhere = new LogicalOperatorCondition();
    newWhere.setValues(new ArrayList<>());
    newWhere.setSymbol(LogicalOperatorCondition.ConditionLogicalOperatorSymbol.AND);

    WhereCondition where = aql.getWhere();
    if (where != null) {
      newWhere.getValues().add(where);
    }

    if (CollectionUtils.isNotEmpty(whereConditions)) {
      newWhere.getValues().addAll(whereConditions);
    }
    aql.setWhere(newWhere);
  }

  protected void extendContainsClause(AqlQuery aql, List<SelectExpression> whereClauseSelectFields, Containment contains, int nextContainmentId,  String attrPath) {
    IdentifiedPath path = new IdentifiedPath();
    path.setPath(AqlObjectPath.parse(attrPath));

    SelectExpression se = new SelectExpression();
    se.setColumnExpression(path);
    whereClauseSelectFields.add(se);

    ContainmentSetOperator newContains = new ContainmentSetOperator();
    newContains.setValues(new ArrayList<>());


    ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
    containmentClassExpression.setType(AqlQueryConstants.COMPOSITION_TYPE);
    containmentClassExpression.setIdentifier("c" + nextContainmentId);
    path.setRoot(containmentClassExpression);

    newContains.setSymbol(ContainmentSetOperatorSymbol.AND);
    newContains.getValues().add(containmentClassExpression);
    newContains.getValues().add(contains);

    ((ContainmentClassExpression) aql.getFrom()).setContains(newContains);
  }

  protected List<Primitive> toSimpleValueList(Collection<String> list) {
    return list.stream()
        .map(StringPrimitive::new)
        .collect(Collectors.toList());
  }

  protected List<String> findCompositionsIdentifier(Containment dto) {
    if (dto == null) {
      return null;
    }

    List<String> compositions = new LinkedList<>();

    Queue<Containment> queue = new ArrayDeque<>();
    queue.add(dto);

    while (!queue.isEmpty()) {
      Containment current = queue.remove();

      if (current instanceof ContainmentSetOperator containmentLogicalOperator) {

        queue.addAll(containmentLogicalOperator.getValues());

      } else if (current instanceof ContainmentClassExpression containmentDto) {

        if (containmentDto.getType().toUpperCase().contains(AqlQueryConstants.COMPOSITION_TYPE)) {
          compositions.add(containmentDto.getIdentifier());
        }

        if (containmentDto.getContains() != null) {
          queue.add(containmentDto.getContains());
        }
      }
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
      } else if (current instanceof ContainmentClassExpression containmentDto) {
        String identifier = containmentDto.getIdentifier();
        String[] identifierId = identifier.split("\\D");
        if (identifierId.length > 2) {
          int currentId = Integer.getInteger(identifierId[1]);
          if (currentId > nextId) {
            nextId = currentId;
          }
        }
        if (containmentDto.getContains() != null) {
          queue.add(containmentDto.getContains());
        }
      }
    }
    return nextId + 1;
  }

  protected void logAqlQuery(Logger log, AqlQuery aql, String logMessage) {
    try {
      log.debug(
              String.format(logMessage,
                      AqlRenderer.render(aql)));
    } catch (Exception e) {
      log.error("Cannot parse aql query while logging", e);
    }
  }
}
