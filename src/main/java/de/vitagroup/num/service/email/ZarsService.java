package de.vitagroup.num.service.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.StudyCategories;
import de.vitagroup.num.domain.dto.ZarsInfoDto;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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
  private final EmailService emailService;
  private final ZarsProperties zarsProperties;

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
  public void registerToZars(@NotNull ZarsInfoDto study) {
    try {
      String csv = generateCSV(study);
      String subject = "Projekt NUM-" + study.getId();
      String body =
          String.format(
              "NUM-%d%nTitel: %s%nProjektleiter: %s%nNeuer Status: %s",
              study.getId(), study.getName(), study.getCoordinator(), study.getStatus().toString());
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
  private String generateCSV(@NotNull ZarsInfoDto study) {
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

  private List<String> generateStudyRow(@NotNull ZarsInfoDto study) {

    List<String> values = new ArrayList<>();
    values.add("NUM-" + study.getId()); // Local project id
    values.add(study.getName()); // Project title
    values.add(study.getCoordinator()); // Project leader
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
    values.add(study.getQueries()); // One or more queries
    values.add(study.getApprovalDate()); // Approval date
    values.add("NA"); // Contract end date
    values.add("NUM"); // Locations
    values.add(study.getPartners()); // Project partners
    values.add(study.isFinanced() ? "Ja" : "Nein"); // Private financing
    values.add("MII Broad Consent"); // Legal grounds
    values.add("NA"); // New end date
    values.add("NA"); // New query
    values.add("NA"); // Date of first data delivery
    values.add("Nein"); // Profitability report?
    values.add(study.getClosedDate()); // Publication date
    values.add("NA"); // Publication reference
    values.add("NA"); // Comments

    return values;
  }
}
