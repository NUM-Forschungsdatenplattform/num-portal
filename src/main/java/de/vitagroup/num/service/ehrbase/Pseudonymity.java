package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class Pseudonymity {

  private final PrivacyProperties privacyProperties;
  private final EhrBaseService ehrBaseService;

  public String getPseudonym(String uuid, Long studyId) {
    return new DigestUtils("SHA3-256")
        .digestAsHex(uuid + studyId + privacyProperties.getPseudonymitySecret());
  }

  public String getEhrIdFromPseudonym(@NotNull String pseudonym, Long studyId) {
    Set<String> ehrIds = ehrBaseService.getAllPatientIds();
    for (String ehrId : ehrIds) {
      if (pseudonym.equals(getPseudonym(ehrId, studyId))) {
        return ehrId;
      }
    }
    throw new ResourceNotFound("Ehr Id matching the pseudonym was not found");
  }

}
