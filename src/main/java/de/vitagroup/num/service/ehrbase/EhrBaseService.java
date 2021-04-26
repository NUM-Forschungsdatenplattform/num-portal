package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.ParameterValue;
import org.ehrbase.aql.dto.select.SelectDto;
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.aql.dto.select.SelectStatementDto;
import org.ehrbase.aql.parser.AqlToDtoParser;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/** Service using the EhrBaseSDK to talk to the EhrBaseAPI */
@Slf4j
@Service
public class EhrBaseService {

  private static final Aql ALL_PATIENTS_IDS =
      Aql.builder().query("select e/ehr_id/value from ehr e").build();

  private static final String COMPOSITION_VALUE = "COMPOSITION";
  private static final String COMPOSITION_KEY = "_type";
  private static final String NAME = "name";
  private static final String PATH = "path";
  private static final String PSEUDONYM = "pseudonym";
  private static final String EHR_ID_PATH = "/ehr_id/value";

  private final DefaultRestClient restClient;
  private final CompositionResponseDataBuilder compositionResponseDataBuilder;
  private final Pseudonymity pseudonymity;

  @Autowired
  public EhrBaseService(
      DefaultRestClient restClient,
      CompositionResponseDataBuilder compositionResponseDataBuilder,
      @Lazy Pseudonymity pseudonymity) {
    this.restClient = restClient;
    this.compositionResponseDataBuilder = compositionResponseDataBuilder;
    this.pseudonymity = pseudonymity;
  }

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
   * Executes a raw aql query
   *
   * @param aqlDto The aql query
   * @return QueryResponseData
   */
  public List<QueryResponseData> executeRawQuery(AqlDto aqlDto, Long studyId) {

    addSelectEhrId(aqlDto);
    Query<Record> query = new AqlBinder().bind(aqlDto).getLeft();

    try {

      QueryResponseData response = restClient.aqlEndpoint().executeRaw(query);
      return flattenIfCompositionPresent(response, studyId);

    } catch (WrongStatusCodeException e) {
      log.error("An error has occurred while calling ehrbase", e);
      throw e;
    } catch (ClientException e) {
      log.error("An error has occurred while calling ehrbase", e);
      throw new SystemException("An error has occurred, cannot execute aql");
    }
  }

  private void addSelectEhrId(AqlDto aqlDto) {
    SelectDto selectDto = aqlDto.getSelect();
    List<SelectStatementDto> selectStatementDtos = selectDto.getStatement();
    SelectFieldDto ehrIdStatement = new SelectFieldDto();
    ehrIdStatement.setAqlPath(EhrFields.EHR_ID().getPath());
    ehrIdStatement.setContainmentId(aqlDto.getEhr().getContainmentId());
    selectStatementDtos.add(0, ehrIdStatement);
    selectDto.setStatement(selectStatementDtos);
  }

  public Set<String> getAllPatientIds() {
    return retrieveEligiblePatientIds(ALL_PATIENTS_IDS);
  }

  public List<TemplateMetaDataDto> getAllTemplatesMetadata() {
    TemplatesResponseData templateResponseData = restClient.templateEndpoint().findAllTemplates();
    return templateResponseData.get();
  }

  private List<QueryResponseData> flattenIfCompositionPresent(
      QueryResponseData response, Long studyId) {
    List<Map<String, Object>> compositions = new ArrayList<>();

    List<String> ehrIds = getAndRemoveEhrIdColumn(response);

    for (List<Object> row : response.getRows()) {
      for (Object cell : row) {
        if (isComposition(cell)) {
          compositions.add((Map<String, Object>) cell);
        }
      }
    }

    List<QueryResponseData> listOfResponseData;

    if (compositions.isEmpty()) {
      log.debug("No compositions in the response. Returning raw ehr response");
      listOfResponseData = List.of(response);
    } else {

      listOfResponseData = compositionResponseDataBuilder.build(compositions);
    }

    addPseudonyms(ehrIds, listOfResponseData, studyId);

    return listOfResponseData;
  }

  private void addPseudonyms(
      List<String> ehrIds, List<QueryResponseData> listOfResponseData, Long studyId) {
    List<String> pseudonyms =
        ehrIds.stream()
            .map(ehrId -> pseudonymity.getPseudonym(ehrId, studyId))
            .collect(Collectors.toList());
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

  private List<String> getAndRemoveEhrIdColumn(QueryResponseData compositions) {

    List<Map<String, String>> columns = compositions.getColumns();
    if (columns == null || columns.size() < 2) {
      throw new BadRequestException("No data columns in the query result");
    }
    String ehrIdPath = columns.get(0).get(PATH);
    if (ehrIdPath == null || !ehrIdPath.equals(EHR_ID_PATH)) {
      throw new SystemException("query result doesn't contain ehrId column");
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
        && ((Map<String, String>) object).get(COMPOSITION_KEY).equals(COMPOSITION_VALUE);
  }
}
