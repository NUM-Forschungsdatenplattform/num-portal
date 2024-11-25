package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.CohortDto;
import org.highmed.numportal.domain.model.ExportType;
import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.ProjectStatus;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.service.atna.AtnaService;
import org.highmed.numportal.service.exception.SystemException;
import org.highmed.numportal.service.util.ExportUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ERROR_WHILE_RETRIEVING_DATA;


@Service
@Slf4j
@AllArgsConstructor
public class ManagerService {

  private final UserDetailsService userDetailsService;

  private final AtnaService atnaService;

  private final CohortService cohortService;

  private final ExportUtil exportUtil;

  private final ObjectMapper mapper;


  public String executeManagerProject(CohortDto cohortDto, List<String> templates, String userId) {
    var queryResponse = StringUtils.EMPTY;
    var project = createManagerProject();
    try {
      userDetailsService.checkIsUserApproved(userId);
      var templateMap = CollectionUtils.isNotEmpty(templates) ? templates.stream().collect(Collectors.toMap(k -> k, v -> v)) : Collections.emptyMap();
      List<QueryResponseData> responseData =
          exportUtil.executeDefaultConfiguration(
              project.getId(), cohortService.toCohort(cohortDto), (Map<String, String>) templateMap);
      queryResponse = mapper.writeValueAsString(responseData);

    } catch (Exception e) {
      atnaService.logDataExport(userId, project.getId(), project, false);
      throw new SystemException(ProjectService.class, ERROR_WHILE_RETRIEVING_DATA,
          String.format(ERROR_WHILE_RETRIEVING_DATA, e.getLocalizedMessage()));
    }
    atnaService.logDataExport(userId, project.getId(), project, true);
    return queryResponse;
  }

  public StreamingResponseBody getManagerExportResponseBody(CohortDto cohortDto, List<String> templates, String userId, ExportType format) {
    userDetailsService.checkIsUserApproved(userId);
    var project = createManagerProject();

    var templateMap = templates.stream().collect(Collectors.toMap(k -> k, v -> v));

    List<QueryResponseData> response =
        exportUtil.executeDefaultConfiguration(
            project.getId(), cohortService.toCohort(cohortDto), templateMap);

    if (format == ExportType.json) {
      return exportUtil.exportJson(response);
    } else {
      return exportUtil.exportCsv(response, project.getId());
    }
  }

  private Project createManagerProject() {
    var undef = "undef";
    return Project.builder()
                  .id(0L)
                  .name("Manager data retrieval project")
                  .createDate(OffsetDateTime.now())
                  .startDate(LocalDate.now())
                  .description("Adhoc temp project for manager data retrieval")
                  .goal(undef)
                  .usedOutsideEu(false)
                  .firstHypotheses(undef)
                  .secondHypotheses(undef)
                  .description("Temporary project for manager data retrieval")
                  .coordinator(UserDetails.builder().userId(undef).organization(Organization.builder().id(0L).build()).build())
                  .status(ProjectStatus.DENIED)
                  .build();
  }


}
