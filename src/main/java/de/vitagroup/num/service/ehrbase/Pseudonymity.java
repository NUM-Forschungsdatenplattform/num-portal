package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.EHR_ID_MATCHING_THE_PSEUDONYM_WAS_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PSEUDONYMITY_SECRET_IS_NOT_CONFIGURED;

@Component
@AllArgsConstructor
public class Pseudonymity {

  private final PrivacyProperties privacyProperties;
  private final EhrBaseService ehrBaseService;

  public String getPseudonym(String uuid, Long projectId) {
    if (privacyProperties.getPseudonymitySecret() == null) {
      throw new SystemException(Pseudonymity.class, PSEUDONYMITY_SECRET_IS_NOT_CONFIGURED);
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
    throw new ResourceNotFound(Pseudonymity.class, EHR_ID_MATCHING_THE_PSEUDONYM_WAS_NOT_FOUND);
  }
}
