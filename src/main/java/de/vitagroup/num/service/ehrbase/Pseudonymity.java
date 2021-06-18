package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
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

  public String getPseudonym(String uuid, Long projectId) {
    if (privacyProperties.getPseudonymitySecret() == null) {
      throw new SystemException("Pseudonymity secret is not configured");
    }
    return new DigestUtils("SHA3-256")
        .digestAsHex(uuid + projectId + privacyProperties.getPseudonymitySecret());
  }

  public String getEhrIdFromPseudonym(@NotNull String pseudonym, Long projectId) {
    Set<String> ehrIds = ehrBaseService.getAllPatientIds();
    for (String ehrId : ehrIds) {
      if (pseudonym.equals(getPseudonym(ehrId, projectId))) {
        return ehrId;
      }
    }
    throw new ResourceNotFound("Ehr Id matching the pseudonym was not found");
  }
}
