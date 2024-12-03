package org.highmed.numportal.service.atna;

import org.highmed.numportal.domain.model.Project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openehealth.ipf.commons.audit.AuditException;
import org.openehealth.ipf.commons.audit.DefaultAuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.codes.XspaPoUCode;
import org.openehealth.ipf.commons.audit.event.DataExportBuilder;
import org.openehealth.ipf.commons.audit.model.ActiveParticipantType;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.openehealth.ipf.commons.audit.model.TypeValuePairType;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.springframework.stereotype.Service;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

@Service
@Slf4j
public class AtnaService {

  private static final String EVENT_CODE_DATA_EXPORT = "110106";
  private static final String SYSTEM_NAME = "Num portal";
  private final AtnaProperties properties;
  private final ObjectMapper mapper;
  private DefaultAuditContext auditContext;

  public AtnaService(AtnaProperties properties, ObjectMapper mapper) {
    this.properties = properties;
    this.mapper = mapper;
  }

  @PostConstruct
  private void initialize() {
    auditContext = new DefaultAuditContext();
    auditContext.setAuditEnabled(properties.isEnabled());
    auditContext.setAuditRepositoryPort(properties.getPort());
    auditContext.setAuditRepositoryHost(properties.getHost());
  }

  public void logDataExport(
      String userId, Long projectId, @Nullable Project project, boolean successful) {
    AuditMessage auditMessage =
        new DataExportBuilder(
            successful ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MajorFailure,
            EventType.of(EVENT_CODE_DATA_EXPORT, SYSTEM_NAME, "Export"),
            XspaPoUCode.Research)
            .addActiveParticipant(new ActiveParticipantType(userId, true))
            .addStudyParticipantObject(String.valueOf(projectId), getProjectDetails(project))
            .setAuditSource(auditContext)
            .getMessage();

    validateAndSend(auditMessage);
  }

  private List<TypeValuePairType> getProjectDetails(@Nullable Project project) {

    if (project == null) {
      return List.of();
    }

    try {
      Long organizationId = project.getCoordinator().getOrganization().getId();
      return List.of(
          new TypeValuePairType("Name", project.getName()),
          new TypeValuePairType("First hypothesis", project.getFirstHypotheses()),
          new TypeValuePairType("Second hypothesis", project.getSecondHypotheses()),
          new TypeValuePairType("Coordinator user id", project.getCoordinator().getUserId()),
          new TypeValuePairType(
              "Coordinator organization id",
              organizationId != null ? organizationId.toString() : StringUtils.EMPTY),
          new TypeValuePairType("Status", project.getStatus().name()),
          new TypeValuePairType("Create date", project.getCreateDate().toString()));
    } catch (Exception e) {
      log.debug("Cannot extract project information", e);
      return List.of();
    }
  }

  private void validateAndSend(AuditMessage auditMessage) {
    auditMessage.getParticipantObjectIdentifications();
    try {
      auditMessage.validate();
    } catch (AuditException e) {
      try {
        log.debug(
            "Failed to log atna message {} with cause", mapper.writeValueAsString(auditMessage), e);
      } catch (JsonProcessingException ex) {
        log.debug("Failed to log message", ex);
      }
    }
    auditContext.audit(auditMessage);
  }
}
