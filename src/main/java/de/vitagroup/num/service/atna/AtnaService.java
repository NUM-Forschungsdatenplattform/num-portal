package de.vitagroup.num.service.atna;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Study;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.openehealth.ipf.commons.audit.AuditException;
import org.openehealth.ipf.commons.audit.DefaultAuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.codes.XspaPoUCode;
import org.openehealth.ipf.commons.audit.event.DataExportBuilder;
import org.openehealth.ipf.commons.audit.model.ActiveParticipantType;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.openehealth.ipf.commons.audit.model.TypeValuePairType;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AtnaService {

  private DefaultAuditContext auditContext;

  @Autowired private AtnaProperties properties;

  @Autowired private ObjectMapper mapper;

  private static final String EVENT_CODE_DATA_EXPORT = "110106";
  private static final String SYSTEM_NAME = "Num portal";

  @PostConstruct
  private void initialize() {
    auditContext = new DefaultAuditContext();
    auditContext.setAuditEnabled(properties.isEnabled());
    auditContext.setAuditRepositoryPort(properties.getPort());
    auditContext.setAuditRepositoryHost(properties.getHost());
  }

  public void logDataExport(String userId, Study study, boolean successful) {
    AuditMessage auditMessage =
        new DataExportBuilder(
                successful ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MajorFailure,
                EventType.of(EVENT_CODE_DATA_EXPORT, SYSTEM_NAME, "Export"),
                XspaPoUCode.Research)
            .addActiveParticipant(new ActiveParticipantType(userId, true))
            .addStudyParticipantObject(String.valueOf(study.getId()), getStudyDetails(study))
            .setAuditSource(auditContext)
            .getMessage();

    validateAndSend(auditMessage);
  }

  private List<TypeValuePairType> getStudyDetails(Study study) {
    try {
      return List.of(
          new TypeValuePairType("Name", study.getName()),
          new TypeValuePairType("First hypothesis", study.getFirstHypotheses()),
          new TypeValuePairType("Second hypothesis", study.getSecondHypotheses()),
          new TypeValuePairType("Coordinator user id", study.getCoordinator().getUserId()),
          new TypeValuePairType(
              "Coordinator organization id", study.getCoordinator().getOrganizationId()),
          new TypeValuePairType("Status", study.getStatus().name()),
          new TypeValuePairType("Create date", study.getCreateDate().toString()));
    } catch (Exception e) {
      log.debug("Cannot extract study information", e);
      return List.of();
    }
  }

  private void validateAndSend(AuditMessage auditMessage) {
    auditMessage.getParticipantObjectIdentifications();
    try {
      auditMessage.validate();
    } catch (AuditException e) {
      try {
        log.debug("Failed to log atna message", mapper.writeValueAsString(auditMessage), e);
      } catch (JsonProcessingException ex) {
        log.debug("Failed to log message", ex);
      }
    }
    auditContext.audit(auditMessage);
  }
}
