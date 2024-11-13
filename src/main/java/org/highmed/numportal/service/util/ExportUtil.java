package org.highmed.numportal.service.util;

import org.highmed.numportal.domain.model.Cohort;
import org.highmed.numportal.properties.ConsentProperties;
import org.highmed.numportal.properties.PrivacyProperties;
import org.highmed.numportal.service.CohortService;
import org.highmed.numportal.service.ProjectService;
import org.highmed.numportal.service.TemplateService;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.ehrbase.ResponseFilter;
import org.highmed.numportal.service.exception.PrivacyException;
import org.highmed.numportal.service.exception.ResourceNotFound;
import org.highmed.numportal.service.exception.SystemException;
import org.highmed.numportal.service.policy.EhrPolicy;
import org.highmed.numportal.service.policy.EuropeanConsentPolicy;
import org.highmed.numportal.service.policy.Policy;
import org.highmed.numportal.service.policy.ProjectPolicyService;
import org.highmed.numportal.service.policy.TemplatesPolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.AN_ISSUE_HAS_OCCURRED_CANNOT_EXECUTE_AQL;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ERROR_CREATING_A_ZIP_FILE_FOR_DATA_EXPORT;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ERROR_WHILE_CREATING_THE_CSV_FILE;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.RESULTS_WITHHELD_FOR_PRIVACY_REASONS;

@Slf4j
@AllArgsConstructor
@Component
public class ExportUtil {

  private static final String CSV_FILE_PATTERN = "%s_%s.csv";

  private final CohortService cohortService;

  private final TemplateService templateService;

  private final EhrBaseService ehrBaseService;

  private final ResponseFilter responseFilter;

  private final PrivacyProperties privacyProperties;

  private final ConsentProperties consentProperties;

  private final ProjectPolicyService projectPolicyService;

  private final ObjectMapper mapper;

  public String getExportFilenameBody(Long projectId) {
    return String.format(
                     "Project_%d_%s",
                     projectId,
                     LocalDateTime.now()
                                  .truncatedTo(ChronoUnit.MINUTES)
                                  .format(DateTimeFormatter.ISO_LOCAL_DATE))
                 .replace('-', '_');
  }

  public List<QueryResponseData> executeDefaultConfiguration(Long projectId, Cohort cohort, Map<String, String> templates) {
    if (templates == null || templates.isEmpty()) {
      return List.of();
    }
    Set<String> ehrIds = cohortService.executeCohort(cohort, false);

    if (ehrIds.size() < privacyProperties.getMinHits()) {
      log.warn(RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
      throw new PrivacyException(ProjectService.class, RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
    }

    List<QueryResponseData> response = new LinkedList<>();

    templates.forEach(
        (templateId, v) ->
            response.addAll(retrieveTemplateData(ehrIds, templateId, projectId, false)));
    return responseFilter.filterResponse(response);
  }

  private List<QueryResponseData> retrieveTemplateData(
      Set<String> ehrIds, String templateId, Long projectId, Boolean usedOutsideEu) {
    try {
      AqlQuery aql = templateService.createSelectCompositionQuery(templateId);

      List<Policy> policies =
          collectProjectPolicies(ehrIds, Map.of(templateId, templateId), usedOutsideEu);
      projectPolicyService.apply(aql, policies);

      List<QueryResponseData> response = ehrBaseService.executeRawQuery(aql, projectId);
      response.forEach(data -> data.setName(templateId));
      return response;

    } catch (ResourceNotFound e) {
      log.error("Could not retrieve data for template {} and project {}. Failed with message {} ", templateId, projectId, e.getMessage(), e);
      log.error(e.getMessage(), e);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    QueryResponseData response = new QueryResponseData();
    response.setName(templateId);
    return List.of(response);
  }

  public List<Policy> collectProjectPolicies(
      Set<String> ehrIds, Map<String, String> templates, boolean usedOutsideEu) {
    List<Policy> policies = new LinkedList<>();
    policies.add(EhrPolicy.builder().cohortEhrIds(ehrIds).build());
    policies.add(TemplatesPolicy.builder().templatesMap(templates).build());

    if (usedOutsideEu) {
      policies.add(
          EuropeanConsentPolicy.builder()
                               .oid(consentProperties.getAllowUsageOutsideEuOid())
                               .build());
    }

    return policies;
  }

  public StreamingResponseBody exportJson(List<QueryResponseData> response) {
    String json;
    try {
      json = mapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new SystemException(ProjectService.class, AN_ISSUE_HAS_OCCURRED_CANNOT_EXECUTE_AQL);
    }
    return outputStream -> {
      outputStream.write(json.getBytes());
      outputStream.flush();
      outputStream.close();
    };
  }

  public StreamingResponseBody exportCsv(List<QueryResponseData> response, Long projectId) {
    return outputStream ->
        streamResponseAsZip(response, getExportFilenameBody(projectId), outputStream);
  }

  public void streamResponseAsZip(
      List<QueryResponseData> queryResponseDataList,
      String filenameStart,
      OutputStream outputStream) {

    try (var zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {

      var index = 0;
      for (QueryResponseData queryResponseData : queryResponseDataList) {

        String responseName = queryResponseData.getName();
        if (StringUtils.isEmpty(responseName)) {
          responseName = String.valueOf(index);
        }
        zipOutputStream.putNextEntry(
            new ZipEntry(String.format(CSV_FILE_PATTERN, filenameStart, responseName)));
        addResponseAsCsv(zipOutputStream, queryResponseData);
        zipOutputStream.closeEntry();
        index++;
      }
    } catch (IOException e) {
      log.error("Error creating a zip file for data export.", e);
      throw new SystemException(ProjectService.class, ERROR_CREATING_A_ZIP_FILE_FOR_DATA_EXPORT,
          String.format(ERROR_CREATING_A_ZIP_FILE_FOR_DATA_EXPORT, e.getLocalizedMessage()));
    }
  }

  private void addResponseAsCsv(ZipOutputStream zipOutputStream, QueryResponseData queryResponseData) {
    List<String> paths = new ArrayList<>();

    for (Map<String, String> column : queryResponseData.getColumns()) {
      paths.add(column.get("path"));
    }
    CSVPrinter printer;
    try {
      printer =
          CSVFormat.EXCEL.builder()
                         .setHeader(paths.toArray(new String[]{}))
                         .build()
                         .print(new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8));

      for (List<Object> row : queryResponseData.getRows()) {
        printer.printRecord(row);
      }
      printer.flush();
    } catch (IOException e) {
      throw new SystemException(ProjectService.class, ERROR_WHILE_CREATING_THE_CSV_FILE,
          String.format(ERROR_WHILE_CREATING_THE_CSV_FILE, e.getMessage()));
    }
  }

}
