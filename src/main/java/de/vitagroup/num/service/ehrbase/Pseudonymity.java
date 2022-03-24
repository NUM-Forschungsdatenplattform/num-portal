package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.properties.PrivacyProperties;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class Pseudonymity {

  private static final String ORIGINAL = "original";
  private static final String PSEUDONYM = "pseudonym";
  private final PrivacyProperties privacyProperties;
  private final EhrBaseService ehrBaseService;

  public List<String> getPseudonyms(List<String> secondLevelPseudonyms, Long projectId) {
    var parameters = intiParameters(projectId);

    secondLevelPseudonyms.forEach(
        secondLevelPseudonym -> parameters.addParameter(ORIGINAL, secondLevelPseudonym));

    var thirdLevelPseudonyms = retrievePseudonyms(parameters);

    return secondLevelPseudonyms.stream().map(original -> {
      var result = findPseudonymForOriginal(thirdLevelPseudonyms, original);
      return result.orElse(null);
    }).collect(Collectors.toList());
  }

  private Parameters retrievePseudonyms(Parameters parameters) {
    return new Parameters();
  }

  private Parameters intiParameters(Long projectId) {
    Parameters parameters = new Parameters();
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
