package de.vitagroup.num.service.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyCategories;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.StudyTransition;
import de.vitagroup.num.domain.repository.StudyTransitionRepository;
import de.vitagroup.num.service.UserService;
import de.vitagroup.num.service.executors.CohortQueryLister;
import de.vitagroup.num.web.exception.SystemException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@ConditionalOnBean(ZarsProperties.class)
public class ZarsService {

  private final ObjectMapper objectMapper;
  private final UserService userService;
  private final StudyTransitionRepository studyTransitionRepository;
  private final EmailService emailService;
  private final ZarsProperties zarsProperties;
  private final CohortQueryLister cohortQueryLister;

  private String[] zarsHeaders;

  @PostConstruct
  public void initialize() {
    try {
      File resource = new ClassPathResource("ZARSHeaders.json").getFile();
      String json = new String(Files.readAllBytes(resource.toPath()));
      zarsHeaders = objectMapper.readValue(json, String[].class);
    } catch (IOException e) {
      log.error("Failed to read ZARS headers file, can't send updates to ZARS!");
    }
  }

  @Async
  public void registerToZars(@NotNull Study study) {
    try {
      String csv = generateCSV(study);
      String subject = "Projekt NUM-" + study.getId();
      String body =
          String.format(
              "NUM-%d%nTitel: %s%nProjektleiter: %s%nNeuer Status: %s",
              study.getId(), study.getName(), getCoordinator(study), study.getStatus().toString());
      emailService.sendEmailWithAttachment(
          subject,
          body,
          zarsProperties.getEmail(),
          csv,
          "NUM_" + study.getId() + ".csv",
          "text/csv");
      log.debug("Registration email successfully sent to " + zarsProperties.getEmail());
    } catch (Exception e) {
      log.error("Could not send email to ZARS", e);
    }
  }

  @NotNull
  private String generateCSV(@NotNull Study study) {
    if (zarsHeaders == null) {
      log.error("ZARS headers file reading has failed, can't send updates to ZARS!");
      return StringUtils.EMPTY;
    }
    StringWriter writer = new StringWriter();
    try (CSVPrinter printer = CSVFormat.EXCEL.withHeader(zarsHeaders).print(writer)) {

      printer.printRecord(generateStudyRow(study));
      printer.flush();
    } catch (IOException e) {
      log.error("Error while creating the ZARS CSV file", e);
    }

    return writer.toString();
  }

  private List<String> generateStudyRow(@NotNull Study study) {

    List<String> values = new ArrayList<>();
    values.add("NUM-" + study.getId()); // Local project id
    values.add(study.getName()); // Project title
    values.add(getCoordinator(study)); // Project leader
    values.add(study.getStartDate().toString()); // Start date
    values.add(study.getEndDate().toString()); // End date
    values.add(study.getGoal()); // Goal
    values.add(study.getDescription()); // Description
    values.add(study.getSimpleDescription()); // Simple description
    values.add(String.join(", ", study.getKeywords())); // Keywords
    values.add(
        study.getCategories().stream()
            .map(StudyCategories::toString)
            .collect(Collectors.joining(", "))); // Categories
    values.add(getQueries(study)); // One or more queries
    values.add(getApprovalDateIfExists(study)); // Approval date
    values.add("NA"); // Contract end date
    values.add("NUM"); // Locations
    values.add(getPartners(study)); // Project partners
    values.add(study.isFinanced() ? "Ja" : "Nein"); // Private financing
    values.add("MII Broad Consent"); // Legal grounds
    values.add("NA"); // New end date
    values.add("NA"); // New query
    values.add("NA"); // Date of first data delivery
    values.add("Nein"); // Profitability report?
    values.add(getClosedDateIfExists(study)); // Publication date
    values.add("NA"); // Publication reference
    values.add("NA"); // Comments

    return values;
  }

  @NotNull
  private String getCoordinator(@NotNull Study study) {
    return userService.getUserById(study.getCoordinator().getUserId(), false).getUsername();
  }

  @NotNull
  private String getQueries(Study study) {
    if (study.getCohort() == null) {
      return StringUtils.EMPTY;
    }
    return String.join(", ", cohortQueryLister.list(study.getCohort()));
  }

  @NotNull
  private String getApprovalDateIfExists(Study study) {
    List<StudyTransition> transitions =
        studyTransitionRepository
            .findAllByStudyIdAndFromStatusAndToStatus(
                study.getId(), StudyStatus.REVIEWING, StudyStatus.APPROVED)
            .orElse(Collections.emptyList());
    if (transitions.size() > 1) {
      log.error("More than one transition from REVIEWING to APPROVED for study " + study.getId());
      return StringUtils.EMPTY;
    }
    if (transitions.size() == 1) {
      return transitions.get(0).getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    return StringUtils.EMPTY;
  }

  @NotNull
  private String getPartners(Study study) {
    Set<Organization> organizations = new HashSet<>();

    if (study.getCoordinator().getOrganization() != null) {
      organizations.add(study.getCoordinator().getOrganization());
    }
    study
        .getResearchers()
        .forEach(
            userDetails -> {
              if (userDetails.getOrganization() != null) {
                organizations.add(userDetails.getOrganization());
              }
            });
    return organizations.stream().map(Organization::getName).collect(Collectors.joining(", "));
  }

  @NotNull
  private String getClosedDateIfExists(Study study) {
    List<StudyTransition> transitions =
        studyTransitionRepository
            .findAllByStudyIdAndFromStatusAndToStatus(
                study.getId(), StudyStatus.PUBLISHED, StudyStatus.CLOSED)
            .orElse(Collections.emptyList());
    if (transitions.size() > 1) {
      throw new SystemException(
          "More than one transition from PUBLISHED to CLOSED for study " + study.getId());
    }
    if (transitions.size() == 1) {
      return transitions.get(0).getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    return StringUtils.EMPTY;
  }
}
