package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.web.exception.SystemException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.aql.record.Record1;
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

  private final DefaultRestClient restClient;

  /**
   * Retrieves a distinct list of patient ids for the given aql
   *
   * @param aql The aql to retrieve patient ids for
   * @return A distinct list of patient ids
   * @throws WrongStatusCodeException in case if a malformed aql
   */
  public Set<String> retrieveEligiblePatientIds(Aql aql) {
    Query<Record1<UUID>> query = Query.buildNativeQuery(aql.getQuery(), UUID.class);
    try {
      List<Record1<UUID>> results = restClient.aqlEndpoint().execute(query);
      return results.stream().map(result -> result.value1().toString()).collect(Collectors.toSet());
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
    Query query = Query.buildNativeQuery(aql.getQuery());
    try {
      return restClient.aqlEndpoint().executeRaw(query);
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
}
