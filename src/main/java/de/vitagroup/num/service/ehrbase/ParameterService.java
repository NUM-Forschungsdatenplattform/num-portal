package de.vitagroup.num.service.ehrbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import de.vitagroup.num.domain.dto.ParameterOptionsDto;
import de.vitagroup.num.service.AqlService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterService {

  public static final String TYPE = "_type";
  public static final String TERMINOLOGY_ID = "terminology_id";
  public static final String VALUE = "value";
  public static final String CODE_STRING = "code_string";
  public static final String SYMBOL = "symbol";
  public static final String DEFINING_CODE = "defining_code";
  private static final String SELECT = "Select";
  private static final String SELECT_DISTINCT = "Select distinct";
  private final ObjectMapper mapper;

  /** Create the aql query for retrieving all distinct existing values of a certain aql path */
  public String createQuery(String aqlPath, String archetypeId) {
    var aql = new AqlDto();

    var selectFieldDto = new SelectFieldDto();
    selectFieldDto.setAqlPath(aqlPath);
    selectFieldDto.setContainmentId(1);

    var select = new SelectDto();
    select.setStatement(List.of(selectFieldDto));

    var contains = new ContainmentDto();
    contains.setArchetypeId(archetypeId);
    contains.setId(1);

    var orderByExpressionDto = new OrderByExpressionDto();
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

  public ParameterOptionsDto getSimpleParameterOptions(
      QueryResponseData response, String aqlPathPostfix) {

    var parameterOptionsDto = new ParameterOptionsDto();

    if (response != null && CollectionUtils.isNotEmpty(response.getRows())) {
      response.getRows().forEach(row -> processSimpleRow(parameterOptionsDto, row, aqlPathPostfix));
      return parameterOptionsDto;
    }
    return null;
  }

  public ParameterOptionsDto getParameterOptions(QueryResponseData responseData, String postfix) {
    var parameterOptionsDto = new ParameterOptionsDto();
    responseData.getRows().forEach(row -> processResponseRow(parameterOptionsDto, row, postfix));
    return parameterOptionsDto;
  }

  private void processSimpleRow(ParameterOptionsDto options, List<?> row, String pathPostfix) {
    if (row != null) {
      try {
        var json = mapper.writeValueAsString(row.get(0));
        options.setType(JsonPath.read(json, "value._type"));
        options.setUnit(JsonPath.read(json, "value.units"));
        var value = JsonPath.read(json, pathPostfix.replace("/", "."));
        options.getOptions().put(value.toString(), value);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
  }

  private void processResponseRow(
      ParameterOptionsDto parameterOptionsDto, List<Object> row, String postfix) {
    if (CollectionUtils.isNotEmpty(row) && (row.get(0) instanceof HashMap)) {
      var rowData = (HashMap<?, ?>) row.get(0);

      if (rowData.containsKey(SYMBOL)) {
        var data = (HashMap<?, ?>) rowData.get(SYMBOL);

        var definingCode = (HashMap<?, ?>) data.get(DEFINING_CODE);

        parameterOptionsDto.setType(definingCode.get(TYPE).toString());
        var codeString = definingCode.get(CODE_STRING);

        parameterOptionsDto
            .getOptions()
            .put(
                codeString,
                ((LinkedHashMap<?, ?>) definingCode.get(TERMINOLOGY_ID)).get(VALUE)
                    + "-"
                    + codeString);
        return;
      }

      if (AqlService.VALUE_DEFINING_CODE.equals(postfix)) {

        try {
          var json = mapper.writeValueAsString(rowData);
          postfix = postfix.replace("/", ".");
          postfix = postfix.substring(1);
          parameterOptionsDto.setType(JsonPath.read(json, postfix + "._type"));

          var codeString = JsonPath.read(json, postfix + ".code_string");
          var terminologyId = JsonPath.read(json, postfix + ".terminology_id.value");
          parameterOptionsDto.getOptions().put(codeString, terminologyId + "-" + codeString);
          return;
        } catch (JsonProcessingException e) {
          log.error("Parameter data could not be retrieved.");
        }
      }

      if ("CODE_PHRASE".equals(rowData.get(TYPE))) {
        parameterOptionsDto.setType(rowData.get(TYPE).toString());
        var codeString = rowData.get(CODE_STRING).toString();

        parameterOptionsDto
            .getOptions()
            .put(
                codeString,
                ((LinkedHashMap<?, ?>) rowData.get(TERMINOLOGY_ID)).get(VALUE) + "-" + codeString);
      }
    }
  }

  private String insertSelect(String query) {
    var result = StringUtils.substringAfter(query, SELECT);
    return new StringBuilder(result).insert(0, SELECT_DISTINCT).toString();
  }
}
