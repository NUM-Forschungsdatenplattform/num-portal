package de.vitagroup.num.service.ehrbase;

import ca.uhn.fhir.context.FhirContext;
import de.vitagroup.num.properties.FttpProperties;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

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

  public List<String> getPseudonyms(List<String> secondLevelPseudonyms, Long projectId) {
    var parameters = intiParameters(projectId);

    secondLevelPseudonyms = secondLevelPseudonyms.stream().map(this::tempFormatId).collect(Collectors.toList());

    secondLevelPseudonyms.forEach(original -> parameters.addParameter(ORIGINAL, original));

    var thirdLevelPseudonyms = retrievePseudonyms(parameters);

    if (thirdLevelPseudonyms.isPresent()) {
      var params = thirdLevelPseudonyms.get();
      if (!params.getParameters("error").isEmpty()) {
        throw new ResourceNotFound(PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
      }
      return secondLevelPseudonyms.stream().map(original -> {
        var result = findPseudonymForOriginal(params, original);
        return result.orElse(null);
      }).collect(Collectors.toList());
    } else {
      throw new ResourceNotFound(PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
    }
  }

  private String tempFormatId(String id) {
    if (id.length() < 2) {
      id = id + "2";
    }
    id = id.substring(0, 2);
    return "codex_CQ1A" + id;
  }

  private Optional<Parameters> retrievePseudonyms(Parameters parameters) {
    var request = new HttpPost(fttpProperties.getUrl());
    request.setHeader("Content-Type", FHIR_CONTENT_TYPE);

    try {
      request.setEntity(
          new StringEntity(fhirContext.newXmlParser().encodeResourceToString(parameters),
              ContentType.parse(FHIR_CONTENT_TYPE)));
      var response = httpClient.execute(request);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        return Optional.of(fhirContext.newXmlParser().parseResource(Parameters.class, response.getEntity().getContent()));
      }
    } catch (Exception e) {
      throw new ResourceNotFound(PSEUDONYMS_COULD_NOT_BE_RETRIEVED_MESSAGE);
    }

    return Optional.empty();
  }

  private Parameters intiParameters(Long projectId) {
    var parameters = new Parameters();
    parameters.addParameter("study", new StringType("num"));
    parameters.addParameter("source", new StringType("codex"));
    parameters.addParameter("target", new StringType("extern_" + projectId));
    parameters.addParameter("apikey", new StringType("iCZdh7ZWuf8ms)vvBgU-IaLi4"));
    parameters.addParameter("event", new StringType("num.get_extern_psn"));
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
