package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.domain.dto.ParameterOptionsDto;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.containment.ContainmentDto;
import org.ehrbase.aql.dto.orderby.OrderByExpressionDto;
import org.ehrbase.aql.dto.orderby.OrderByExpressionSymbol;
import org.ehrbase.aql.dto.select.SelectDto;
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.response.openehr.QueryResponseData;
import org.springframework.stereotype.Service;

@Service
public class ParameterService {

  private static final String SELECT = "Select";
  private static final String SELECT_DISTINCT = "Select distinct";

  /**
   * Create the aql query for retrieving all distinct existing values of a certain aql path
   *
   * @param aqlPath
   * @param archetypeId
   * @return
   */
  public String createQuery(String aqlPath, String archetypeId) {
    AqlDto aql = new AqlDto();

    SelectFieldDto selectFieldDto = new SelectFieldDto();
    selectFieldDto.setAqlPath(aqlPath);
    selectFieldDto.setContainmentId(1);

    SelectDto select = new SelectDto();
    select.setStatement(List.of(selectFieldDto));

    ContainmentDto contains = new ContainmentDto();
    contains.setArchetypeId(archetypeId);
    contains.setId(1);

    OrderByExpressionDto orderByExpressionDto = new OrderByExpressionDto();
    orderByExpressionDto.setStatement(selectFieldDto);
    orderByExpressionDto.setSymbol(OrderByExpressionSymbol.ASC);

    List<OrderByExpressionDto> orderByList = new LinkedList<>();
    orderByList.add(orderByExpressionDto);
    aql.setSelect(select);
    aql.setContains(contains);
    aql.setOrderBy(orderByList);

    String query = new AqlBinder().bind(aql).getLeft().buildAql();
    return insertSelect(query);
  }

  public ParameterOptionsDto getParameterOptions(
      QueryResponseData response, String aqlPath, String archetypeId) {
    List<Object> options = new LinkedList<>();
    if (response != null && CollectionUtils.isNotEmpty(response.getRows())) {
      List<List<Object>> rows = response.getRows();
      rows.forEach(
          row -> {
            if (CollectionUtils.isNotEmpty(row) && (row.get(0) instanceof HashMap)) {
              LinkedHashMap rowData = (LinkedHashMap) row.get(0);

              if (isDvText(rowData) || isDvBoolean(rowData)) {
                Object value = getValue(rowData);
                if (value != null) {
                  options.add(value);
                }
              }
            }
          });
    }
    return ParameterOptionsDto.builder()
        .options(options)
        .aqlPath(aqlPath)
        .archetypeId(archetypeId)
        .build();
  }

  private Object getValue(LinkedHashMap rowData) {
    if (rowData.containsKey("value")) {
      return rowData.get("value");
    } else {
      return null;
    }
  }

  private boolean isDvText(LinkedHashMap rowData) {
    return isType(rowData, "DV_CODED_TEXT");
  }

  private boolean isDvBoolean(LinkedHashMap rowData) {
    return isType(rowData, "DV_BOOLEAN");
  }

  private boolean isType(LinkedHashMap rowData, String ehrType) {
    if (rowData.containsKey("symbol")) {
      if (rowData.get("symbol") instanceof HashMap) {
        LinkedHashMap symbolData = (LinkedHashMap) rowData.get("symbol");
        if (symbolData.containsKey("_type") && symbolData.get("_type").equals(ehrType)) {
          System.out.println("It's a _type -> " + ehrType);
          return true;
        }
      }
    }
    return false;
  }

  private String insertSelect(String query) {
    String result = StringUtils.substringAfter(query, SELECT);
    return new StringBuilder(result).insert(0, SELECT_DISTINCT).toString();
  }
}
