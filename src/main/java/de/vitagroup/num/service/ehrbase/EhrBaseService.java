package de.vitagroup.num.service.ehrbase;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.client.aql.query.Query;
import org.ehrbase.client.aql.record.Record1;
import org.ehrbase.client.exception.WrongStatusCodeException;
import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.springframework.stereotype.Service;

/**
 * Service using the EhrBaseSDK to talk to the EhrBaseAPI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EhrBaseService {

  private static final String PATIENT_ID_AQL = "select e/ehr_id/value from ehr e";

  private final DefaultRestClient restClient;

  /**
   * Retrieves a distinct list of patient ids for the given aql
   *
   * @param aql The aql to retrieve patient ids for
   * @return A distinct list of patient ids
   * @throws WrongStatusCodeException in case if a malformed aql
   */
  public List<String> getPatientIds(String aql) {
    Query<Record1<UUID>> query = Query.buildNativeQuery(aql, UUID.class);
    try {
      List<Record1<UUID>> results = restClient.aqlEndpoint().execute(query);
      return results.stream()
          .map(result -> result.value1().toString())
          .distinct()
          .collect(Collectors.toList());
    } catch (WrongStatusCodeException e) {
      log.error("Malformed query exception", e);
      throw e;
    }
  }
}
