package de.vitagroup.num.service.ehrbase;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.ParameterValue;
import org.ehrbase.aql.dto.select.SelectDto;
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.aql.parser.AqlParseException;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.ehrbase.aqleditor.service.AqlEditorAqlService;
import org.ehrbase.client.aql.field.EhrFields;
import org.ehrbase.client.aql.query.EntityQuery;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.aql.record.Record;
import org.ehrbase.client.exception.ClientException;
import org.ehrbase.client.exception.WrongStatusCodeException;
import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.ehrbase.response.openehr.QueryResponseData;
import org.ehrbase.response.openehr.TemplatesResponseData;
import org.springframework.stereotype.Service;

/** Service using the EhrBaseSDK to talk to the EhrBaseAPI */
@Slf4j
@Service
@RequiredArgsConstructor
public class EhrBaseService {

  private static final Aql ALL_PATIENTS_IDS =
      Aql.builder().query("select e/ehr_id/value from ehr e").build();

  private static final String COMPOSITION_VALUE = "COMPOSITION";
  private static final String COMPOSITION_KEY = "_type";

  private final DefaultRestClient restClient;
  private final ObjectMapper mapper;
  private final CompositionResponseDataBuilder compositionResponseDataBuilder;
  private final AqlEditorAqlService aqlEditorAqlService;

  /**
   * Retrieves a distinct list of patient ids for the given aql
   *
   * @param aql The aql to retrieve patient ids for
   * @return A distinct list of patient ids
   * @throws WrongStatusCodeException in case if a malformed aql
   */
  public Set<String> retrieveEligiblePatientIds(Aql aql) {

    AqlDto dto = new AqlToDtoParser().parse(aql.getQuery());
    SelectFieldDto selectStatementDto = new SelectFieldDto();
    selectStatementDto.setAqlPath(EhrFields.EHR_ID().getPath());
    selectStatementDto.setContainmentId(dto.getEhr().getContainmentId());
    SelectDto selectDto = new SelectDto();
    selectDto.setStatement(List.of(selectStatementDto));
    dto.setSelect(selectDto);
    Pair<EntityQuery<Record>, List<ParameterValue>> pair = new AqlBinder().bind(dto);

    try {
      List<Record> results = restClient.aqlEndpoint().execute(pair.getLeft());
      return results.stream().map(result -> result.value(0).toString()).collect(Collectors.toSet());
    } catch (WrongStatusCodeException e) {
      log.error("Malformed query exception", e);
      throw e;
    }
  }

  /**
   * Executes an aql query
   *
   * @param aql The aql query
   * @return QueryResponseData
   */
  public QueryResponseData executeAql(Aql aql) {
    return executeRawQuery(aql.getQuery());
  }

  /**
   * Executes a raw aql query
   *
   * @param queryString The aql query string
   * @return QueryResponseData
   */
  public QueryResponseData executeRawQuery(String queryString) {

    validateQuery(queryString);
    Query query = Query.buildNativeQuery(queryString);

    try {

      QueryResponseData response = restClient.aqlEndpoint().executeRaw(query);
      return flattenIfCompositionPresent(response);

    } catch (WrongStatusCodeException e) {
      log.error("An error has occurred while calling ehrbase", e);
      throw e;
    } catch (ClientException e) {
      log.error("An error has occurred while calling ehrbase", e);
      throw new SystemException("An error has occurred, cannot execute aql");
    }
  }

  public Set<String> getAllPatientIds() {
    return retrieveEligiblePatientIds(ALL_PATIENTS_IDS);
  }

  public List<TemplateMetaDataDto> getAllTemplatesMetadata() {
    TemplatesResponseData templateResponseData = restClient.templateEndpoint().findAllTemplates();
    return templateResponseData.get();
  }

  private void validateQuery(String queryString) {
    if (StringUtils.isEmpty(queryString)) {
      throw new BadRequestException("Empty aql query string");
    }
    try {
      new AqlToDtoParser().parse(queryString);
    } catch (AqlParseException e) {
      throw new BadRequestException("Invalid aql: " + e.getMessage());
    }
  }

  private QueryResponseData flattenIfCompositionPresent(QueryResponseData response) {
    List<Map<String, String>> compositions = new ArrayList<>();

    for (List<Object> row : response.getRows()) {
      for (Object cell : row) {
        if (isComposition(cell)) {
          compositions.add((Map<String, String>) cell);
        } else {
          log.debug("Executing query containing mixed data types. Returning raw ehr response");
          return response;
        }
      }
    }

    if (compositions.isEmpty()) {
      log.debug("No compositions in the response. Returning raw ehr response");
      return response;
    }

    QueryResponseData flattenedResponse = compositionResponseDataBuilder.build(compositions);
    flattenedResponse.setQuery(response.getQuery());
    return flattenedResponse;
  }

  private boolean isComposition(Object object) {
    return object instanceof Map
        && ((Map<String, String>) object).containsKey(COMPOSITION_KEY)
        && ((Map<String, String>) object).get(COMPOSITION_KEY).equals(COMPOSITION_VALUE);
  }
}
