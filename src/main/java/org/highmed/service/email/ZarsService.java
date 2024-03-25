package org.highmed.service.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.highmed.domain.dto.ZarsInfoDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@ConditionalOnBean(ZarsProperties.class)
public class ZarsService {

  public static final String STATUS = "status";
  private final ObjectMapper objectMapper;
  private final EmailService emailService;
  private final ZarsProperties zarsProperties;
  private final MessageSource messageSource;

  private String[] zarsHeaders;

  @PostConstruct
  public void initialize() {
    try {
      var resource = new ClassPathResource("ZARSHeaders.json").getFile();
      var json = new String(Files.readAllBytes(resource.toPath()));
      zarsHeaders = objectMapper.readValue(json, String[].class);
    } catch (IOException e) {
      log.error("Failed to read ZARS headers file, can't send updates to ZARS!");
    }
  }

  @Async
  public void registerToZars(@NotNull ZarsInfoDto zarsInfoDto) {
    try {
      String csv = generateCSV(zarsInfoDto);
      String status = translate(STATUS, zarsInfoDto.getStatus().toString());
      String subject = status + ": " + zarsInfoDto.getName();
      var body =
          String.format(
              "NUM-%d%nTitel: %s%nProjektleiter: %s%nNeuer Status: %s",
              zarsInfoDto.getId(), zarsInfoDto.getName(), zarsInfoDto.getCoordinator(), status);
      emailService.sendEmailWithAttachment(
          subject,
          body,
          zarsProperties.getEmail(),
          csv,
          "NUM_" + zarsInfoDto.getId() + ".csv",
          "text/csv");
      log.debug("Registration email successfully sent to " + zarsProperties.getEmail());
    } catch (Exception e) {
      log.error("Could not send email to ZARS", e);
    }
  }

  @NotNull
  private String generateCSV(@NotNull ZarsInfoDto zarsInfoDto) {
    if (zarsHeaders == null) {
      log.error("ZARS headers file reading has failed, can't send updates to ZARS!");
      return StringUtils.EMPTY;
    }
    var writer = new StringWriter();
    try (CSVPrinter printer = CSVFormat.EXCEL.builder()
            .setHeader(zarsHeaders)
            .build()
            .print(writer)) {

      printer.printRecord(generateProjectRow(zarsInfoDto));
      printer.flush();
    } catch (IOException e) {
      log.error("Error while creating the ZARS CSV file", e);
    }

    return writer.toString();
  }

  private List<String> generateProjectRow(@NotNull ZarsInfoDto zarsInfoDto) {

    List<String> values = new ArrayList<>();
    values.add("NUM-" + zarsInfoDto.getId()); // Local project id
    values.add(zarsInfoDto.getName()); // Project title
    values.add(zarsInfoDto.getCoordinator()); // Project leader
    values.add(zarsInfoDto.getStartDate().toString()); // Start date
    values.add(zarsInfoDto.getEndDate().toString()); // End date
    values.add(zarsInfoDto.getGoal()); // Goal
    values.add(zarsInfoDto.getDescription()); // Description
    values.add(zarsInfoDto.getSimpleDescription()); // Simple description
    values.add(String.join(", ", zarsInfoDto.getKeywords())); // Keywords
    values.add(
        zarsInfoDto.getCategories().stream()
            .map(category -> translate("category", category.toString()))
            .collect(Collectors.joining(", "))); // Categories
    values.add(zarsInfoDto.getQueries()); // One or more queries
    values.add(zarsInfoDto.getApprovalDate()); // Approval date
    values.add("NA"); // Contract end date
    values.add("NUM"); // Locations
    values.add(zarsInfoDto.getPartners()); // Project partners
    values.add(zarsInfoDto.isFinanced() ? "Ja" : "Nein"); // Private financing
    values.add("MII Broad Consent"); // Legal grounds
    values.add("NA"); // New end date
    values.add("NA"); // New query
    values.add("NA"); // Date of first data delivery
    values.add("Nein"); // Profitability report?
    values.add(zarsInfoDto.getClosedDate()); // Publication date
    values.add("NA"); // Publication reference
    values.add("NA"); // Comments
    values.add(translate(STATUS, zarsInfoDto.getStatus().toString()));

    return values;
  }

  private String translate(String prefix, String key) {
    return messageSource.getMessage(prefix + "." + key.toLowerCase(), null, Locale.GERMAN);
  }
}
