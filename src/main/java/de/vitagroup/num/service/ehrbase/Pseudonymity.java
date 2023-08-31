package de.vitagroup.num.service.ehrbase;

import ca.uhn.fhir.context.FhirContext;
import de.vitagroup.num.properties.FttpProperties;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.properties.PseudonymsPsnWorkflowProperties;
import de.vitagroup.num.service.exception.ResourceNotFound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class Pseudonymity {

  private static final String ORIGINAL = "original";
  private static final String PSEUDONYM = "pseudonym";
  private static final String FHIR_CONTENT_TYPE = "application/fhir+xml;charset=utf-8";
  private static final String PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE =
      "Pseudonyms could not be retrieved";
  private static final String REQUEST_FAILED_RETRIEVED_MESSAGE =
          "Request to retrieve pseudonyms failed";

  // is just a guessing...because there are also codes that start with codex_ but we do not receive the pseudonym back (example codex_A12CB2)
  private static final String EXTERNAL_REF_ID_REGEX_GREIFSWALD_COMPLIANT = "codex_[A-Z0-9-]{6}";

  private static final String DIGEST_ALGORITHM = "SHA3-256";
  private final CloseableHttpClient httpClient;
  private final FttpProperties fttpProperties;
  private final FhirContext fhirContext;
  private final PseudonymsPsnWorkflowProperties pseudonymsPsnWorkflowProperties;
  private final PrivacyProperties privacyProperties;

  @Value("${pseudonymity.fake3rdPartyPseudonymEnabled:false}")
  private boolean fake3rdPartyPseudonymEnabled;

  public List<String> getPseudonyms(List<String> secondLevelPseudonyms, Long projectId) {
    List<String> result = new LinkedList<>();
    // I guess Greisfwald restricted the number of original params per request
    List<List<String>> chunks = ListUtils.partition(secondLevelPseudonyms, privacyProperties.getPseudonomityChunksSize());
    chunks.forEach(listChunks -> result.addAll(getPseudonymsData(listChunks, projectId)));
    return result;
  }

  private List<String> getPseudonymsData(List<String> secondLevelPseudonyms, Long projectId) {
    var parameters = initParameters(projectId);

    secondLevelPseudonyms.forEach(original -> {
      if (Pattern.matches(EXTERNAL_REF_ID_REGEX_GREIFSWALD_COMPLIANT, original)) {
        parameters.addParameter(ORIGINAL, original);
      }
    });

    var thirdLevelPseudonyms = retrievePseudonyms(parameters);

    if (thirdLevelPseudonyms.isPresent()) {
      var params = thirdLevelPseudonyms.get();
      if (!params.getParameters("error").isEmpty()) {
        log.warn("Could not retrieve pseudonyms for secondLevelPseudonyms {} ", parameters.getParameters(ORIGINAL));
        // this might be removed when API on Greisfwald side is ready and working for any kind of id
        if (Boolean.TRUE.equals(fake3rdPartyPseudonymEnabled)) {
          return generateNumThirdLevelPseudonym(secondLevelPseudonyms, projectId);
        } else {
          throw new ResourceNotFound(Pseudonymity.class, PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
        }
      }
      Map<String, Parameters.ParametersParameterComponent> paramsData = groupPseudonyms(params);
      return secondLevelPseudonyms.stream()
              .map(original -> findPseudonymForOriginal(paramsData, original, projectId).get())
              .collect(Collectors.toList());
    } else {
      if (Boolean.TRUE.equals(fake3rdPartyPseudonymEnabled)) {
        // something did not work on Greisfwald side, so generate fake 3rd party pseudonyms
        // this might be removed when API on Greisfwald side is ready and working for any kind of id
        return generateNumThirdLevelPseudonym(secondLevelPseudonyms, projectId);
      } else {
        throw new ResourceNotFound(Pseudonymity.class, PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
      }
    }
  }

  private Optional<Parameters> retrievePseudonyms(Parameters parameters) {
    if (CollectionUtils.isNotEmpty(parameters.getParameters(ORIGINAL))) {
      var request = new HttpPost(fttpProperties.getUrl());
      request.setHeader("Content-Type", FHIR_CONTENT_TYPE);
      CloseableHttpResponse response = null;
      try {
        String requestBody = fhirContext.newXmlParser().encodeResourceToString(parameters);
        request.setEntity(new StringEntity(requestBody, ContentType.parse(FHIR_CONTENT_TYPE)));
        response = httpClient.execute(request);
        log.debug("Request pseudonyms with body: {} ", requestBody);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          String resp = EntityUtils.toString(response.getEntity());
          log.debug("Received pseudonyms response: {} ", resp);
          return Optional.of(fhirContext.newXmlParser().parseResource(Parameters.class, resp));
        } else {
          log.error("Could not retrieve pseudonyms. Expected status code 200, received {} with response body: {} ",
                  response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity()));
          throw new ResourceNotFound(Pseudonymity.class, REQUEST_FAILED_RETRIEVED_MESSAGE);
        }
      } catch (Exception e) {
        log.error("Could not retrieve pseudonyms ", e);
        throw new ResourceNotFound(Pseudonymity.class, PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
      } finally {
        if (response != null) {
          try {
            response.close();
          } catch (IOException e) {
            log.warn("Could not close response from {} ", fttpProperties.getUrl());
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * <a> https://simplifier.net/guide/ttp-fhir-gateway-ig/markdown-WorkflowBasierteVerwaltung-Operations-requestPsnWorkflow?version=current </a>
   * @param projectId
   * @return
   */
  private Parameters initParameters(Long projectId) {
    var parameters = new Parameters();
    parameters.addParameter("study", pseudonymsPsnWorkflowProperties.getStudy());
    parameters.addParameter("source", pseudonymsPsnWorkflowProperties.getSource());
    parameters.addParameter("target", pseudonymsPsnWorkflowProperties.getTarget() + projectId);
    parameters.addParameter("apikey", pseudonymsPsnWorkflowProperties.getApiKey());
    parameters.addParameter("event",  pseudonymsPsnWorkflowProperties.getEvent());
    return parameters;
  }

  private Optional<String> findPseudonymForOriginal(Map<String, Parameters.ParametersParameterComponent> parameters, String original, Long projectId) {
    if (Pattern.matches(EXTERNAL_REF_ID_REGEX_GREIFSWALD_COMPLIANT, original) && parameters.containsKey(original)) {
      var param = parameters.get(original);
      String pseudonym = getPartValue(PSEUDONYM, param);
      if (StringUtils.isNotEmpty(pseudonym)) {
        return Optional.of(pseudonym);
      } else {
        if (Boolean.TRUE.equals(fake3rdPartyPseudonymEnabled)) {
          return Optional.of(generateNumThirdLevelPseudonym(original, projectId));
        } else {
          throw new ResourceNotFound(Pseudonymity.class, PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
        }
      }
    } else {
      if (Boolean.TRUE.equals(fake3rdPartyPseudonymEnabled)) {
        return Optional.of(generateNumThirdLevelPseudonym(original, projectId));
      } else {
        throw new ResourceNotFound(Pseudonymity.class, PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
      }
    }
  }

  private List<String> generateNumThirdLevelPseudonym(List<String> secondLevelPseudonyms, Long projectId) {
    return secondLevelPseudonyms.stream()
            .map(original -> generateNumThirdLevelPseudonym(original, projectId))
            .collect(Collectors.toList());
  }

  private String generateNumThirdLevelPseudonym(String original, Long projectId) {
    log.debug("For id {} was generated fake 3rd level pseudonym", original);
    return new DigestUtils(DIGEST_ALGORITHM)
            .digestAsHex(original + projectId + privacyProperties.getPseudonymitySecret());
  }

  private String getPartValue(String value, Parameters.ParametersParameterComponent param) {
    for (var part : param.getPart()) {
      if (value.equals(part.getName()) && part.getValue() != null) {
        Identifier identifier = (Identifier) part.getValue();
        return identifier.getValue() != null ? identifier.getValue() : StringUtils.EMPTY;
      }
    }
    return StringUtils.EMPTY;
  }

  private Map<String, Parameters.ParametersParameterComponent> groupPseudonyms(Parameters params) {
    Map<String, Parameters.ParametersParameterComponent> paramsData = new HashMap<>();
    for (var param : params.getParameter()) {
      String org = getPartValue(ORIGINAL, param);
      paramsData.put(org, param);
    }
    return paramsData;
  }
}
