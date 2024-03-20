package org.highmed.service.ehrbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.datavalues.DvBoolean;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.SingleValuedDataValue;
import com.nedap.archie.rm.datavalues.quantity.DvCount;
import com.nedap.archie.rm.datavalues.quantity.DvOrdinal;
import com.nedap.archie.rm.datavalues.quantity.DvQuantity;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDuration;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import org.highmed.domain.dto.ParameterOptionsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.orderby.OrderByExpression;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectClause;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.TemporalAccessorDeSerializer;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.VersionUidDeSerializer;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.ArchieObjectMapperProvider;
import org.highmed.service.UserDetailsService;
import org.highmed.service.util.AqlQueryConstants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.temporal.TemporalAccessor;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterService {

  private static final String VALUE_DEFINING_CODE = "/value/defining_code/code_string";

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
    var objectMapper = ArchieObjectMapperProvider.getObjectMapper().copy();
    var module = new SimpleModule("openEHR", new Version(1, 0, 0, null, null, null));
    module.addDeserializer(ObjectVersionId.class, new VersionUidDeSerializer());
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
      log.trace("Evicting aql parameters options cache");
      cache.clear();
    }
  }

  private ParameterOptionsDto getParameters(String aqlPath, String archetypeId, String postfix) {
    String query;
    if(aqlPath.startsWith("/")) {
      query =
              createQueryString(aqlPath.substring(1, aqlPath.length() - postfix.length()), archetypeId);
    } else {
      query =
              createQueryString(aqlPath.substring(0, aqlPath.length() - postfix.length()), archetypeId);
    }
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
                  log.debug("[AQL parameter] query response data row {} ", rowString);
                  var element =
                          (SingleValuedDataValue<?>)
                                  buildAqlObjectMapper().readValue(rowString, RMObject.class);
                  if (Objects.nonNull(element.getValue())) {
                    if (element.getValue().getClass().isAssignableFrom(DvCodedText.class)) {
                      convertDvCodedText((DvCodedText) element.getValue(), parameterOptions, postfix);
                    } else if (element.getValue().getClass().isAssignableFrom(DvQuantity.class)) {
                      convertDvQuantity((DvQuantity) element.getValue(), parameterOptions, postfix);
                    } else if (element.getValue().getClass().isAssignableFrom(DvOrdinal.class)) {
                      convertDvOrdinal((DvOrdinal) element.getValue(), parameterOptions, postfix);
                    } else if (element.getValue().getClass().isAssignableFrom(DvBoolean.class)) {
                      convertDvBoolean(parameterOptions);
                    } else if (element.getValue().getClass().isAssignableFrom(DvDate.class)) {
                      convertDvDate(parameterOptions);
                    } else if (element.getValue().getClass().isAssignableFrom(DvDateTime.class)) {
                      convertDvDateTime(parameterOptions);
                    } else if (element.getValue().getClass().isAssignableFrom(DvTime.class)) {
                      convertTime(parameterOptions);
                    } else if (element.getClass().isAssignableFrom(DvDateTime.class)) {
                      // workaround for openEHR-EHR-OBSERVATION.blood_pressure.v2 and aqlPath:
                      ///data[at0001]/events[at0006]/time/value
                      convertDvDateTime(parameterOptions);
                    } else if (element.getValue().getClass().isAssignableFrom(DvCount.class)) {
                      parameterOptions.setType("DV_COUNT");
                    } else if (element.getValue().getClass().isAssignableFrom(DvDuration.class)) {
                      parameterOptions.setType("DV_DURATION");
                    }
                  }
                }
              } catch (JsonProcessingException e) {
                log.error("Could not retrieve parameters for aqlPath {} and archetypeId {} ", aqlPath, archetypeId, e);
              }
            });

    parameterOptions.setAqlPath(aqlPath);
    parameterOptions.setArchetypeId(archetypeId);
    return parameterOptions;
  }

  /** Create the aql query for retrieving all distinct existing values of a certain aql path */
  private String createQueryString(String aqlPath, String archetypeId) {
    var aql = new AqlQuery();

    ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
    containmentClassExpression.setIdentifier("c0");
    IdentifiedPath path = new IdentifiedPath();
    path.setPath(AqlObjectPath.parse(aqlPath));
    path.setRoot(containmentClassExpression);

    // generate select expression
    SelectClause selectClause = new SelectClause();
    SelectExpression se = new SelectExpression();
    se.setColumnExpression(path);
    se.setAlias("F1");
    selectClause.setStatement(List.of(se));
    selectClause.setDistinct(true);

    // generate from expression
    ContainmentClassExpression from = new ContainmentClassExpression();
    from.setType(AqlQueryConstants.EHR_TYPE);
    from.setIdentifier(AqlQueryConstants.EHR_CONTAINMENT_IDENTIFIER);

    // generate contains expression
    ContainmentClassExpression contains = new ContainmentClassExpression();
    contains.setType(StringUtils.substringBetween(archetypeId, "openEHR-EHR-", "."));
    contains.setIdentifier("c0"+"[" + archetypeId + "]");

    from.setContains(contains);

    var orderByExpressionDto = new OrderByExpression();
    orderByExpressionDto.setStatement(path);
    orderByExpressionDto.setSymbol(OrderByExpression.OrderByDirection.ASC);

    List<OrderByExpression> orderByList = new LinkedList<>();
    orderByList.add(orderByExpressionDto);
    aql.setSelect(selectClause);
    aql.setFrom(from);
    aql.setOrderBy(orderByList);

    return AqlRenderer.render(aql);
  }

  private void convertDvCodedText(DvCodedText data, ParameterOptionsDto dto, String postfix) {
    if (VALUE_MAGNITUDE.equals(postfix)) {
      return;
    }
    dto.setType("DV_CODED_TEXT");
    dto.getOptions().put(data.getDefiningCode().getCodeString(), data.getValue());
  }

  private void convertDvQuantity(DvQuantity data, ParameterOptionsDto dto, String postfix) {
    if (VALUE_DEFINING_CODE.equals(postfix)) {
      return;
    }
    dto.setType("DV_QUANTITY");
    dto.setUnit(data.getUnits());
  }

  private void convertDvOrdinal(DvOrdinal data, ParameterOptionsDto dto, String postfix) {
    if (VALUE_MAGNITUDE.equals(postfix)) {
      return;
    }
    dto.setType("DV_ORDINAL");
    var symbol = data.getSymbol();
    dto.getOptions().put(symbol.getValue(), data.getValue());
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
}
