package de.vitagroup.num.service.ehrbase;

import ca.uhn.fhir.context.FhirContext;
import de.vitagroup.num.properties.FttpProperties;
import de.vitagroup.num.properties.PseudonymsPsnWorkflow;
import de.vitagroup.num.service.exception.ResourceNotFound;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class Pseudonymity {

  private static final String ORIGINAL = "original";
  private static final String PSEUDONYM = "pseudonym";
  private static final String FHIR_CONTENT_TYPE = "application/fhir+xml;charset=utf-8";
  private static final String PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE =
      "Pseudonyms could not be retrieved";
  private final CloseableHttpClient httpClient;
  private final FttpProperties fttpProperties;
  private final FhirContext fhirContext;

  private final PseudonymsPsnWorkflow pseudonymsPsnWorkflow;

  public List<String> getPseudonyms(List<String> secondLevelPseudonyms, Long projectId) {
    var parameters = initParameters(projectId);

    secondLevelPseudonyms.forEach(original -> parameters.addParameter(ORIGINAL, original));

    var thirdLevelPseudonyms = retrievePseudonyms(parameters);

    if (thirdLevelPseudonyms.isPresent()) {
      var params = thirdLevelPseudonyms.get();
      if (!params.getParameters("error").isEmpty()) {
        throw new ResourceNotFound(Pseudonymity.class, PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
      }
      return secondLevelPseudonyms.stream().map(original -> {
        var result = findPseudonymForOriginal(params, original);
        return result.orElse(null);
      }).collect(Collectors.toList());
    } else {
      throw new ResourceNotFound(Pseudonymity.class, PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
    }
  }

  private Optional<Parameters> retrievePseudonyms(Parameters parameters) {
    var request = new HttpPost(fttpProperties.getUrl());
    request.setHeader("Content-Type", FHIR_CONTENT_TYPE);

    try {
      String requestBody = fhirContext.newXmlParser().encodeResourceToString(parameters);
      request.setEntity(new StringEntity(requestBody, ContentType.parse(FHIR_CONTENT_TYPE)));
      var response = httpClient.execute(request);
      //log.debug("Request body {} ", requestBody);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        String resp = EntityUtils.toString(response.getEntity());
        //log.debug("Response {} ", resp);
        return Optional.of(fhirContext.newXmlParser().parseResource(Parameters.class, resp));
      }
    } catch (Exception e) {
      log.error("Could not retrieve pseudonyms {}", e);
      throw new ResourceNotFound(Pseudonymity.class, PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
    }

    return Optional.empty();
  }

  /**
   * https://simplifier.net/guide/ttp-fhir-gateway-ig/markdown-WorkflowBasierteVerwaltung-Operations-requestPsnWorkflow?version=current
   * @param projectId
   * @return
   */
  private Parameters initParameters(Long projectId) {
    var parameters = new Parameters();
    parameters.addParameter("study", pseudonymsPsnWorkflow.getStudy());
    parameters.addParameter("source", pseudonymsPsnWorkflow.getSource());
    parameters.addParameter("target", pseudonymsPsnWorkflow.getTarget() + projectId);
    parameters.addParameter("apikey", pseudonymsPsnWorkflow.getApiKey());
    parameters.addParameter("event",  pseudonymsPsnWorkflow.getEvent());
    return parameters;
  }

  private Optional<String> findPseudonymForOriginal(Parameters parameters, String original) {
    for (var param : parameters.getParameter()) {
      if (original.equals(getPartValue(ORIGINAL, param))) {
        return Optional.of(getPartValue(PSEUDONYM, param));
      }
    }
    return Optional.empty();
  }

  private String getPartValue(String value, Parameters.ParametersParameterComponent param) {
    for (var part : param.getPart()) {
      if (part.getName().equals(value)) {
        return ((Identifier) part.getValue()).getValue();
      }
    }
    return StringUtils.EMPTY;
  }
}
