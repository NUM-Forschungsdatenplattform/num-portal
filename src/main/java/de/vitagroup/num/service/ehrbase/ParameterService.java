package de.vitagroup.num.service.ehrbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datavalues.DvBoolean;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.quantity.DvOrdinal;
import com.nedap.archie.rm.datavalues.quantity.DvQuantity;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import de.vitagroup.num.domain.dto.ParameterOptionsDto;
import de.vitagroup.num.service.UserDetailsService;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.containment.ContainmentDto;
import org.ehrbase.aql.dto.orderby.OrderByExpressionDto;
import org.ehrbase.aql.dto.orderby.OrderByExpressionSymbol;
import org.ehrbase.aql.dto.select.SelectDto;
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.client.openehrclient.VersionUid;
import org.ehrbase.client.openehrclient.defaultrestclient.TemporalAccessorDeSerializer;
import org.ehrbase.client.openehrclient.defaultrestclient.VersionUidDeSerializer;
import org.ehrbase.serialisation.jsonencoding.JacksonUtil;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterService {

  private static final String SELECT = "Select";

  private static final String SELECT_DISTINCT = "Select distinct";

  private static final String VALUE_DEFINING_CODE = "/value/defining_code";

  private static final String VALUE_VALUE = "/value/value";

  private static final String VALUE_MAGNITUDE = "/value/magnitude";

  private static final String VALUE_UNIT = "/value/units";

  private static final String VALUE_SYMBOL_VALUE = "/value/symbol/value";

  private static final String VALUE = "/value";

  private static final String PARAMETERS_CACHE = "aqlParameters";

  private final CacheManager cacheManager;

  private final EhrBaseService ehrBaseService;

  private final UserDetailsService userDetailsService;

  private static ObjectMapper buildAqlObjectMapper() {
    var objectMapper = JacksonUtil.getObjectMapper();
    var module = new SimpleModule("openEHR", new Version(1, 0, 0, null, null, null));
    module.addDeserializer(VersionUid.class, new VersionUidDeSerializer());
    module.addDeserializer(TemporalAccessor.class, new TemporalAccessorDeSerializer());
    objectMapper.registerModule(module);
    return objectMapper;
  }

  @CachePut(value = PARAMETERS_CACHE, key = "#aqlPath")
  public ParameterOptionsDto getParameterValues(String userId, String aqlPath, String archetypeId) {
    userDetailsService.checkIsUserApproved(userId);

    if (aqlPath.endsWith(VALUE_VALUE)) {
      return getParameters(aqlPath, archetypeId, VALUE_VALUE);
    } else if (aqlPath.endsWith(VALUE_MAGNITUDE)) {
      return getParameters(aqlPath, archetypeId, VALUE_MAGNITUDE);
    } else if (aqlPath.endsWith(VALUE_SYMBOL_VALUE)) {
      return getParameters(aqlPath, archetypeId, VALUE_SYMBOL_VALUE);
    } else if (aqlPath.endsWith(VALUE_UNIT)) {
      return getParameters(aqlPath, archetypeId, VALUE_UNIT);
    } else if (aqlPath.endsWith(VALUE_DEFINING_CODE)) {
      return getParameters(aqlPath, archetypeId, VALUE_DEFINING_CODE);
    } else if (aqlPath.endsWith(VALUE)) {
      return getParameters(aqlPath, archetypeId, VALUE);
    } else {
      return getParameters(aqlPath, archetypeId, StringUtils.EMPTY);
    }
  }

  @Scheduled(fixedRate = 3600000)
  public void evictParametersCache() {
    var cache = cacheManager.getCache(PARAMETERS_CACHE);
    if (cache != null) {
      log.trace("Evicting aql parameters opetions cache");
      cache.clear();
    }
  }

  private ParameterOptionsDto getParameters(String aqlPath, String archetypeId, String postfix) {
    var query =
        createQueryString(aqlPath.substring(0, aqlPath.length() - postfix.length()), archetypeId);

    try {
      log.info(
          String.format(
              "[AQL QUERY] Getting parameter %s options with query: %s ", aqlPath, query));
    } catch (Exception e) {
      log.error("Error parsing query while logging", e);
    }

    var parameterOptions = new ParameterOptionsDto();

    var queryResponseData = ehrBaseService.executePlainQuery(query);
    queryResponseData
        .getRows()
        .forEach(
            row -> {
              try {
                if (row.get(0) != null) {
                  var rowString = buildAqlObjectMapper().writeValueAsString(row.get(0));
                  var element =
                      (Element) buildAqlObjectMapper().readValue(rowString, RMObject.class);

                  if (element.getValue().getClass().isAssignableFrom(DvCodedText.class)) {
                    convertDvCodedText((DvCodedText) element.getValue(), parameterOptions);
                  } else if (element.getValue().getClass().isAssignableFrom(DvQuantity.class)) {
                    convertDvQuantity((DvQuantity) element.getValue(), parameterOptions);
                  } else if (element.getValue().getClass().isAssignableFrom(DvOrdinal.class)) {
                    convertDvOrdinal((DvOrdinal) element.getValue(), parameterOptions);
                  } else if (element.getValue().getClass().isAssignableFrom(DvBoolean.class)) {
                    convertDvBoolean(parameterOptions);
                  } else if (element.getValue().getClass().isAssignableFrom(DvDate.class)) {
                    convertDvDate(parameterOptions);
                  } else if (element.getValue().getClass().isAssignableFrom(DvDateTime.class)) {
                    convertDvDateTime(parameterOptions);
                  } else if (element.getValue().getClass().isAssignableFrom(DvTime.class)) {
                    convertTime(parameterOptions);
                  }
                }
              } catch (JsonProcessingException e) {
                log.error("Could not retrieve parameters", e);
              }
            });

    parameterOptions.setAqlPath(aqlPath);
    parameterOptions.setArchetypeId(archetypeId);
    return parameterOptions;
  }

  /** Create the aql query for retrieving all distinct existing values of a certain aql path */
  private String createQueryString(String aqlPath, String archetypeId) {
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

  private void convertDvCodedText(DvCodedText data, ParameterOptionsDto dto) {
    dto.setType("DV_CODED_TEXT");
    dto.getOptions().put(data.getDefiningCode().getCodeString(), data.getValue());
  }

  private void convertDvQuantity(DvQuantity data, ParameterOptionsDto dto) {
    dto.setType("DV_QUANTITY");
    dto.setUnit(data.getUnits());
  }

  private void convertDvOrdinal(DvOrdinal data, ParameterOptionsDto dto) {
    dto.setType("DV_ORDINAL");
    var symbol = data.getSymbol();
    dto.getOptions().put(symbol.getDefiningCode().getCodeString(), symbol.getValue());
  }

  private void convertDvBoolean(ParameterOptionsDto dto) {
    dto.setType("DV_BOOLEAN");
  }

  private void convertDvDate(ParameterOptionsDto dto) {
    dto.setType("DV_DATE");
  }

  private void convertDvDateTime(ParameterOptionsDto dto) {
    dto.setType("DV_DATE_TIME");
  }

  private void convertTime(ParameterOptionsDto dto) {
    dto.setType("DV_TIME");
  }

  private String insertSelect(String query) {
    var result = StringUtils.substringAfter(query, SELECT);
    return new StringBuilder(result).insert(0, SELECT_DISTINCT).toString();
  }
}
