package org.highmed.numportal.service.ehrbase;

import com.nedap.archie.rm.support.identification.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.generator.commons.aql.query.NativeQuery;
import org.ehrbase.openehr.sdk.generator.commons.aql.query.Query;
import org.ehrbase.openehr.sdk.generator.commons.aql.record.Record;
import org.ehrbase.openehr.sdk.generator.commons.aql.record.Record1;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.ehrbase.openehr.sdk.response.dto.TemplatesResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.ehrbase.openehr.sdk.util.exception.ClientException;
import org.ehrbase.openehr.sdk.util.exception.WrongStatusCodeException;
import org.highmed.numportal.domain.model.Aql;
import org.highmed.numportal.properties.EhrBaseProperties;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.SystemException;
import org.highmed.numportal.service.util.AqlQueryConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.*;

/**
 * Service using the EhrBaseSDK to talk to the EhrBaseAPI
 */
@Slf4j
@Service
public class EhrBaseService {

  private static final Aql ALL_PATIENTS_IDS =
      Aql.builder().query("select e/ehr_id/value from ehr e").build();

  private static final String COMPOSITION_KEY = "_type";
  private static final String NAME = "name";
  private static final String PATH = "path";
  private static final String PSEUDONYM = "pseudonym";

  private final DefaultRestClient restClient;
  private final CompositionResponseDataBuilder compositionResponseDataBuilder;
  private final Pseudonymity pseudonymity;
  private final EhrBaseProperties ehrBaseProperties;

  @Autowired
  public EhrBaseService(
      DefaultRestClient restClient,
      CompositionResponseDataBuilder compositionResponseDataBuilder,
      @Lazy Pseudonymity pseudonymity,
      EhrBaseProperties ehrBaseProperties) {
    this.restClient = restClient;
    this.compositionResponseDataBuilder = compositionResponseDataBuilder;
    this.pseudonymity = pseudonymity;
    this.ehrBaseProperties = ehrBaseProperties;
  }

  /**
   * Retrieves a distinct list of patient ids for the given aql
   *
   * @param aql The aql to retrieve patient ids for
   * @return A distinct list of patient ids
   * @throws WrongStatusCodeException in case if a malformed aql
   */
  public Set<String> retrieveEligiblePatientIds(Aql aql) {
    return retrieveEligiblePatientIds(aql.getQuery());
  }

  public Set<String> retrieveEligiblePatientIds(String query) {
    log.debug("EhrBase retrieve ehr ids for query: {} ", query);
    AqlQuery dto = AqlQueryParser.parse(query);
    SelectExpression selectExpression = new SelectExpression();
    IdentifiedPath ehrIdPath = new IdentifiedPath();
    ehrIdPath.setPath(AqlObjectPath.parse(AqlQueryConstants.EHR_ID_PATH));

    ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
    containmentClassExpression.setType(AqlQueryConstants.EHR_TYPE);
    containmentClassExpression.setIdentifier(AqlQueryConstants.EHR_CONTAINMENT_IDENTIFIER);
    ehrIdPath.setRoot(containmentClassExpression);

    selectExpression.setColumnExpression(ehrIdPath);

    dto.getSelect().setStatement(List.of(selectExpression));
    log.info("Generated query for retrieveEligiblePatientIds {} ", AqlRenderer.render(dto));

    try {
      List<Record1<UUID>> results = restClient.aqlEndpoint().execute(Query.buildNativeQuery(AqlRenderer.render(dto), UUID.class));
      return results.stream().map(result -> result.value1().getValue()).collect(Collectors.toSet());
    } catch (WrongStatusCodeException e) {
      log.error(INVALID_AQL_QUERY, e.getMessage(), e);
      throw new WrongStatusCodeException("EhrBaseService.class", 93, 1);
    } catch (ClientException e) {
      log.error(ERROR_MESSAGE, e.getMessage(), e);
      throw new SystemException(EhrBaseService.class, AN_ERROR_HAS_OCCURRED_CANNOT_EXECUTE_AQL,
              String.format(AN_ERROR_HAS_OCCURRED_CANNOT_EXECUTE_AQL, e.getMessage()));
    }
  }

  /**
   * Executes a raw aql query
   * @param aqlDto The aql query
   * @return QueryResponseData
   */
  public List<QueryResponseData> executeRawQuery(AqlQuery aqlDto, Long projectId) {

    addSelectSecondlevelPseudonyms(aqlDto);
    String query = AqlRenderer.render(aqlDto);

    try {
      try {
        log.info(
            String.format(
                "[AQL QUERY] EHR request query: %s ", query));
      } catch (Exception e) {
        log.error("Error parsing query while logging", e);
      }

      log.debug("EhrBase call to execute raw query: {}", query);
      QueryResponseData response = restClient.aqlEndpoint().executeRaw(Query.buildNativeQuery(query));
      return flattenIfCompositionPresent(response, projectId);

    } catch (WrongStatusCodeException e) {
      log.error(INVALID_AQL_QUERY, e.getMessage(), e);
      throw new WrongStatusCodeException("EhrBaseService.class", 94, 1);
    } catch (ClientException e) {
      log.error(ERROR_MESSAGE, e.getMessage(), e);
      throw new SystemException(EhrBaseService.class, AN_ERROR_HAS_OCCURRED_CANNOT_EXECUTE_AQL,
              String.format(AN_ERROR_HAS_OCCURRED_CANNOT_EXECUTE_AQL, e.getMessage()));
    }
  }

  public QueryResponseData executePlainQuery(String queryString) {

    NativeQuery<Record> query = Query.buildNativeQuery(queryString);

    try {
      log.debug("EhrBase call to execute raw query: {}", queryString);
      return restClient.aqlEndpoint().executeRaw(query);
    } catch (WrongStatusCodeException e) {
      log.error(INVALID_AQL_QUERY, e.getMessage(), e);
      throw new WrongStatusCodeException("EhrBaseService.class", 93, 2);
    } catch (ClientException e) {
      log.error(ERROR_MESSAGE, e.getMessage(), e);
      throw new SystemException(EhrBaseService.class, AN_ERROR_HAS_OCCURRED_CANNOT_EXECUTE_AQL,
              String.format(AN_ERROR_HAS_OCCURRED_CANNOT_EXECUTE_AQL, e.getMessage()));
    }
  }

  private void addSelectSecondlevelPseudonyms(AqlQuery aqlDto) {
    SelectExpression selectExpression = new SelectExpression();
    IdentifiedPath ehrIdPath = new IdentifiedPath();
    ehrIdPath.setPath(AqlObjectPath.parse(ehrBaseProperties.getIdPath()));

    ContainmentClassExpression containmentClassExpression = new ContainmentClassExpression();
    containmentClassExpression.setType(AqlQueryConstants.EHR_TYPE);
    containmentClassExpression.setIdentifier(AqlQueryConstants.EHR_CONTAINMENT_IDENTIFIER);
    ehrIdPath.setRoot(containmentClassExpression);

    selectExpression.setColumnExpression(ehrIdPath);
    List<SelectExpression> existingExpressions = new ArrayList<>(aqlDto.getSelect().getStatement());
    existingExpressions.add(0, selectExpression);

    aqlDto.getSelect().setStatement(existingExpressions);
  }

  public Set<String> getAllPatientIds() {
    return retrieveEligiblePatientIds(ALL_PATIENTS_IDS);
  }

  public List<TemplateMetaDataDto> getAllTemplatesMetadata() {
    log.debug("EhrBase call to retrieve all templates ");
    try {
      TemplatesResponseData templateResponseData = restClient.templateEndpoint().findAllTemplates();
      return templateResponseData.get();
    } catch (ClientException e) {
      log.error(ERROR_MESSAGE, e.getMessage(), e);
      throw new SystemException(EhrBaseService.class, AN_ERROR_HAS_OCCURRED_CANNOT_GET_TEMPLATES,
              String.format(AN_ERROR_HAS_OCCURRED_CANNOT_GET_TEMPLATES, e.getMessage()));
    }
  }

  private List<QueryResponseData> flattenIfCompositionPresent(QueryResponseData responseData, Long projectId) {
    List<String> ehrStatusIds = getAndRemoveEhrStatusColumn(responseData);
    List<QueryResponseData> listOfResponseData = flattenCompositions(responseData);
    if (projectId != null) {
      addPseudonyms(ehrStatusIds, listOfResponseData, projectId);
    }
    return listOfResponseData;
  }

  private List<QueryResponseData> flattenCompositions(QueryResponseData responseData) {
    Map<String, List<Map<String, Object>>> compositions = new HashMap<>();
    responseData.setQuery(StringUtils.EMPTY);

    for (List<Object> row : responseData.getRows()) {
      for (int i = 0; i < row.size(); i++) {
        Object cell = row.get(i);
        String name = responseData.getColumns().get(i).get(NAME);

        if (isComposition(cell)) {
          if (compositions.containsKey(name)) {
            compositions.get(name).add((Map<String, Object>) cell);
          } else {
            List<Map<String, Object>> list = new LinkedList<>();
            list.add((Map<String, Object>) cell);
            compositions.put(name, list);
          }
        } else {
          log.debug("Executing query containing mixed data types. Returning raw ehr response");
          return List.of(responseData);
        }
      }
    }

    List<QueryResponseData> aggregatedFlattenedCompositions = new LinkedList<>();

    if (compositions.isEmpty()) {
      log.debug("No compositions in the response. Returning raw ehr response");
      aggregatedFlattenedCompositions = List.of(responseData);
    } else {
      for (Map.Entry<String, List<Map<String, Object>>> entry : compositions.entrySet()) {
        QueryResponseData data = compositionResponseDataBuilder.build(entry.getValue());
        data.setName(entry.getKey());
        aggregatedFlattenedCompositions.add(data);
      }
    }

    return aggregatedFlattenedCompositions;
  }

  private void addPseudonyms(List<String> secondLevelPseudos, List<QueryResponseData> listOfResponseData, Long projectId) {

    List<String> pseudonyms = pseudonymity.getPseudonyms(secondLevelPseudos, projectId);

    Map<String, String> pseudonymityColumn = new HashMap<>();
    pseudonymityColumn.put(PATH, PSEUDONYM);
    pseudonymityColumn.put(NAME, PSEUDONYM);
    for (QueryResponseData queryResponseData : listOfResponseData) {
      queryResponseData.getColumns().add(0, pseudonymityColumn);
      List<List<Object>> rows = queryResponseData.getRows();
      for (int i = 0; i < rows.size(); i++) {
        rows.get(i).add(0, pseudonyms.get(i));
      }
    }
  }

  private List<String> getAndRemoveEhrStatusColumn(QueryResponseData compositions) {

    List<Map<String, String>> columns = compositions.getColumns();
    if (columns == null || columns.size() < 2) {
      throw new BadRequestException(EhrBaseService.class, NO_DATA_COLUMNS_IN_THE_QUERY_RESULT);
    }
    String ehrStatusPath = columns.get(0).get(PATH);
    if (ehrStatusPath == null || !ehrStatusPath.equals("/" + ehrBaseProperties.getIdPath())) {
      throw new SystemException(EhrBaseService.class, QUERY_RESULT_DOESN_T_CONTAIN_EHR_STATUS_COLUMN);
    }
    columns.remove(0);
    return compositions.getRows().stream()
        .map(row -> row.remove(0))
        .map(String.class::cast)
        .collect(Collectors.toList());
  }

  private boolean isComposition(Object object) {
    return object instanceof Map
        && ((Map<String, String>) object).containsKey(COMPOSITION_KEY)
        && ((Map<String, String>) object).get(COMPOSITION_KEY).equals(AqlQueryConstants.COMPOSITION_TYPE);
  }
}
