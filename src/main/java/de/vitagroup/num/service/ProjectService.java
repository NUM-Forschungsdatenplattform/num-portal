package de.vitagroup.num.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.ExportType;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.ProjectTransition;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.dto.ProjectInfoDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.dto.ZarsInfoDto;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.domain.repository.ProjectTransitionRepository;
import de.vitagroup.num.mapper.ProjectMapper;
import de.vitagroup.num.properties.ConsentProperties;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.atna.AtnaService;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.ehrbase.ResponseFilter;
import de.vitagroup.num.service.email.ZarsService;
import de.vitagroup.num.service.executors.CohortQueryLister;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.ProjectApprovalRequestNotification;
import de.vitagroup.num.service.notification.dto.ProjectCloseNotification;
import de.vitagroup.num.service.notification.dto.ProjectStartNotification;
import de.vitagroup.num.service.notification.dto.ProjectStatusChangeNotification;
import de.vitagroup.num.service.policy.EhrPolicy;
import de.vitagroup.num.service.policy.EuropeanConsentPolicy;
import de.vitagroup.num.service.policy.Policy;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import de.vitagroup.num.service.policy.TemplatesPolicy;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.PrivacyException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.ehrbase.response.openehr.QueryResponseData;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectService {

  private static final String PROJECT_NOT_FOUND = "Project not found: ";
  private static final String ZIP_FILE_ENDING = ".zip";
  private static final String JSON_FILE_ENDING = ".json";
  private static final String ZIP_MEDIA_TYPE = "application/zip";
  private static final String CSV_FILE_PATTERN = "%s_%s.csv";

  private final ProjectRepository projectRepository;

  private final UserDetailsService userDetailsService;

  private final EhrBaseService ehrBaseService;

  private final ObjectMapper mapper;

  private final AtnaService atnaService;

  private final UserService userService;

  private final NotificationService notificationService;

  private final ProjectTransitionRepository projectTransitionRepository;

  private final CohortQueryLister cohortQueryLister;

  private final ModelMapper modelMapper;

  @Nullable
  private final ZarsService zarsService;

  private final ProjectPolicyService projectPolicyService;

  private final CohortService cohortService;

  private final ConsentProperties consentProperties;

  private final ResponseFilter responseFilter;

  private final PrivacyProperties privacyProperties;

  private final TemplateService templateService;

  private final ProjectDocCreator projectDocCreator;

  private final ProjectMapper projectMapper;

  private static final String ERROR_MESSAGE = "An issue has occurred, cannot execute aql";

  public void deleteProject(Long projectId, String userId, List<String> roles) {
    userDetailsService.checkIsUserApproved(userId);

    var project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFound(PROJECT_NOT_FOUND + projectId));

    if (project.hasEmptyOrDifferentOwner(userId) && !roles.contains(Roles.SUPER_ADMIN)) {
      throw new ForbiddenException(String.format("Cannot delete project: %s", projectId));
    }

    if (project.isDeletable()) {
      projectRepository.deleteById(projectId);
    } else {
      throw new ForbiddenException(
          String.format(
              "Cannot delete project: %s, invalid status: %s", projectId, project.getStatus()));
    }
  }

  @Transactional
  public void archiveProject(Long projectId, String userId, List<String> roles) {
    var user = userDetailsService.checkIsUserApproved(userId);

    var project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFound(PROJECT_NOT_FOUND + projectId));

    if (project.hasEmptyOrDifferentOwner(userId) && !roles.contains(Roles.SUPER_ADMIN)) {
      throw new ForbiddenException(String.format("Cannot archive project: %s", projectId));
    }

    validateStatus(project.getStatus(), ProjectStatus.ARCHIVED, roles);
    persistTransition(project, project.getStatus(), ProjectStatus.ARCHIVED, user);

    project.setStatus(ProjectStatus.ARCHIVED);
    projectRepository.save(project);
  }

  /**
   * Counts the number of projects existing in the platform
   *
   * @return The count of projects in the platform
   */
  public long countProjects() {
    return projectRepository.count();
  }

  /**
   * Retrieves a list of latest projects information
   *
   * @param count number of projects to be retrieved
   * @return The list of max requested count of latest projects
   */
  public List<ProjectInfoDto> getLatestProjectsInfo(int count, List<String> roles) {

    if (count < 1) {
      return List.of();
    }

    List<Project> projects =
        projectRepository.findLatestProjects(
            count,
            ProjectStatus.APPROVED.name(),
            ProjectStatus.PUBLISHED.name(),
            ProjectStatus.CLOSED.name());
    return projects.stream()
        .map(project -> toProjectInfo(project, roles))
        .collect(Collectors.toList());
  }

  public String retrieveData(
      String query, Long projectId, String userId, Boolean defaultConfiguration) {
    Project project = validateAndRetrieveProject(projectId, userId);

    List<QueryResponseData> responseData;
    if (BooleanUtils.isTrue(defaultConfiguration)) {
      responseData =
          executeDefaultConfiguration(projectId, project.getCohort(), project.getTemplates());
    } else {
      responseData = executeCustomConfiguration(query, projectId, userId);
    }

    try {
      return mapper.writeValueAsString(responseData);
    } catch (JsonProcessingException e) {
      throw new SystemException(ERROR_MESSAGE);
    }
  }

  private List<QueryResponseData> executeDefaultConfiguration(
      Long projectId, Cohort cohort, Map<String, String> templates) {

    if (templates == null || templates.isEmpty()) {
      return List.of();
    }

    Set<String> ehrIds = cohortService.executeCohort(cohort, false);

    if (ehrIds.size() < privacyProperties.getMinHits()) {
      throw new PrivacyException(PrivacyProperties.RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
    }

    List<QueryResponseData> response = new LinkedList<>();

    templates.forEach(
        (templateId, v) ->
            response.addAll(retrieveTemplateData(ehrIds, templateId, projectId, false)));
    return responseFilter.filterResponse(response);
  }

  private List<QueryResponseData> executeCustomConfiguration(
      String query, Long projectId, String userId) {
    List<QueryResponseData> response = executeAql(query, projectId, userId);
    return responseFilter.filterResponse(response);
  }

  private List<QueryResponseData> retrieveTemplateData(
      Set<String> ehrIds, String templateId, Long projectId, Boolean usedOutsideEu) {
    try {
      AqlDto aql = templateService.createSelectCompositionQuery(templateId);

      List<Policy> policies =
          collectProjectPolicies(ehrIds, Map.of(templateId, templateId), usedOutsideEu);
      projectPolicyService.apply(aql, policies);

      List<QueryResponseData> response = ehrBaseService.executeRawQuery(aql, projectId);
      response.forEach(data -> data.setName(templateId));
      return response;

    } catch (ResourceNotFound e) {
      log.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      log.error(e.getMessage(), e);

      QueryResponseData response = new QueryResponseData();
      response.setName(templateId);
      return List.of(response);
    }
  }

  public List<QueryResponseData> executeAql(String query, Long projectId, String userId) {
    List<QueryResponseData> queryResponseData;
    Project project = null;
    try {
      userDetailsService.checkIsUserApproved(userId);

      project = validateAndRetrieveProject(projectId, userId);

      Set<String> ehrIds =
          cohortService.executeCohort(project.getCohort().getId(), project.isUsedOutsideEu());

      AqlDto aql = new AqlToDtoParser().parse(query);

      List<Policy> policies =
          collectProjectPolicies(ehrIds, project.getTemplates(), project.isUsedOutsideEu());
      projectPolicyService.apply(aql, policies);

      queryResponseData = ehrBaseService.executeRawQuery(aql, projectId);

    } catch (Exception e) {
      atnaService.logDataExport(userId, projectId, project, false);
      throw e;
    }
    atnaService.logDataExport(userId, projectId, project, true);
    return queryResponseData;
  }

  public String executeManagerProject(CohortDto cohortDto, List<String> templates, String userId) {
    var queryResponse = StringUtils.EMPTY;
    var project = createManagerProject();
    try {
      userDetailsService.checkIsUserApproved(userId);
      var templateMap = templates.stream().collect(Collectors.toMap(k -> k, v -> v));

      List<QueryResponseData> responseData =
          executeDefaultConfiguration(
              project.getId(), cohortService.toCohort(cohortDto), templateMap);

      queryResponse = mapper.writeValueAsString(responseData);

    } catch (Exception e) {
      atnaService.logDataExport(userId, project.getId(), project, false);
      throw new SystemException("Error while retrieving data:" + e.getLocalizedMessage());
    }
    atnaService.logDataExport(userId, project.getId(), project, true);
    return queryResponse;
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
      throw new SystemException(
          "Error creating a zip file for data export: " + e.getLocalizedMessage());
    }
  }

  private void addResponseAsCsv(
      ZipOutputStream zipOutputStream, QueryResponseData queryResponseData) {
    List<String> paths = new ArrayList<>();

    for (Map<String, String> column : queryResponseData.getColumns()) {
      paths.add(column.get("path"));
    }
    CSVPrinter printer;
    try {
      printer =
          CSVFormat.EXCEL
              .withHeader(paths.toArray(new String[] {}))
              .print(new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8));

      for (List<Object> row : queryResponseData.getRows()) {
        printer.printRecord(row);
      }
      printer.flush();
    } catch (IOException e) {
      throw new SystemException("Error while creating the CSV file");
    }
  }

  public StreamingResponseBody getExportResponseBody(
      String query,
      Long projectId,
      String userId,
      ExportType format,
      Boolean defaultConfiguration) {

    Project project = validateAndRetrieveProject(projectId, userId);
    List<QueryResponseData> response;

    if (BooleanUtils.isTrue(defaultConfiguration)) {
      response =
          executeDefaultConfiguration(projectId, project.getCohort(), project.getTemplates());
    } else {
      response = executeCustomConfiguration(query, projectId, userId);
    }

    if (format == ExportType.json) {
      return exportJson(response);
    } else {
      return exportCsv(response, projectId);
    }
  }

  public StreamingResponseBody getManagerExportResponseBody(
      CohortDto cohortDto, List<String> templates, String userId, ExportType format) {
    userDetailsService.checkIsUserApproved(userId);
    var project = createManagerProject();

    var templateMap = templates.stream().collect(Collectors.toMap(k -> k, v -> v));

    List<QueryResponseData> response =
        executeDefaultConfiguration(
            project.getId(), cohortService.toCohort(cohortDto), templateMap);

    if (format == ExportType.json) {
      return exportJson(response);
    } else {
      return exportCsv(response, project.getId());
    }
  }

  private StreamingResponseBody exportCsv(List<QueryResponseData> response, Long projectId) {
    return outputStream ->
        streamResponseAsZip(response, getExportFilenameBody(projectId), outputStream);
  }

  private StreamingResponseBody exportJson(List<QueryResponseData> response) {
    String json;
    try {
      json = mapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new SystemException(ERROR_MESSAGE);
    }
    return outputStream -> {
      outputStream.write(json.getBytes());
      outputStream.flush();
      outputStream.close();
    };
  }

  public MultiValueMap<String, String> getExportHeaders(ExportType format, Long projectId) {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    String fileEnding;
    if (format == ExportType.json) {
      fileEnding = JSON_FILE_ENDING;
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    } else {
      fileEnding = ZIP_FILE_ENDING;
      headers.add(HttpHeaders.CONTENT_TYPE, ZIP_MEDIA_TYPE);
    }
    headers.add(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=" + getExportFilenameBody(projectId) + fileEnding);
    return headers;
  }

  public Optional<Project> getProjectById(Long projectId) {
    return projectRepository.findById(projectId);
  }

  public boolean exists(Long id) {
    return projectRepository.existsById(id);
  }

  @Transactional
  public Project createProject(ProjectDto projectDto, String userId, List<String> roles) {

    var coordinator = userDetailsService.checkIsUserApproved(userId);

    var project = Project.builder().build();

    validateStatus(null, projectDto.getStatus(), roles);
    persistTransition(project, project.getStatus(), projectDto.getStatus(), coordinator);

    setTemplates(project, projectDto);
    project.setResearchers(getResearchers(projectDto));

    project.setStatus(projectDto.getStatus());
    project.setName(projectDto.getName());
    project.setDescription(projectDto.getDescription());
    project.setSimpleDescription(projectDto.getSimpleDescription());
    project.setUsedOutsideEu(projectDto.isUsedOutsideEu());
    project.setFirstHypotheses(projectDto.getFirstHypotheses());
    project.setSecondHypotheses(projectDto.getSecondHypotheses());
    project.setGoal(projectDto.getGoal());
    project.setCategories(projectDto.getCategories());
    project.setKeywords(projectDto.getKeywords());
    project.setCoordinator(coordinator);
    project.setCreateDate(OffsetDateTime.now());
    project.setModifiedDate(OffsetDateTime.now());
    project.setStartDate(projectDto.getStartDate());
    project.setEndDate(projectDto.getEndDate());
    project.setFinanced(projectDto.isFinanced());

    var savedProject = projectRepository.save(project);

    if (savedProject.getStatus() == ProjectStatus.PENDING) {
      registerToZars(project);
    }

    List<Notification> notifications =
        collectNotifications(
            savedProject,
            savedProject.getStatus(),
            null,
            savedProject.getCoordinator().getUserId(),
            savedProject.getResearchers(),
            savedProject.getResearchers(),
            userId);

    notificationService.send(notifications);

    return savedProject;
  }

  @Transactional
  public Project updateProject(ProjectDto projectDto, Long id, String userId, List<String> roles) {
    var user = userDetailsService.checkIsUserApproved(userId);

    var projectToEdit =
        projectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFound(PROJECT_NOT_FOUND + id));

    if (ProjectStatus.ARCHIVED.equals(projectToEdit.getStatus())
        || ProjectStatus.CLOSED.equals(projectToEdit.getStatus())) {
      throw new ForbiddenException(
          String.format(
              "Cannot update project: %s, invalid project status: %s",
              id, projectToEdit.getStatus()));
    }

    if (CollectionUtils.isNotEmpty(roles)
        && roles.contains(Roles.STUDY_COORDINATOR)
        && projectToEdit.isCoordinator(userId)) {
      return updateProjectAllFields(projectDto, roles, user, projectToEdit);
    } else if (CollectionUtils.isNotEmpty(roles) && roles.contains(Roles.STUDY_APPROVER)) {
      return updateProjectStatus(projectDto, roles, user, projectToEdit);
    } else {
      throw new ForbiddenException("No permissions to edit this project");
    }
  }

  private Project updateProjectStatus(
      ProjectDto projectDto, List<String> roles, UserDetails user, Project projectToEdit) {

    var oldProjectStatus = projectToEdit.getStatus();

    validateStatus(projectToEdit.getStatus(), projectDto.getStatus(), roles);
    persistTransition(projectToEdit, projectToEdit.getStatus(), projectDto.getStatus(), user);
    projectToEdit.setStatus(projectDto.getStatus());

    var savedProject = projectRepository.save(projectToEdit);

    registerToZarsIfNecessary(
        savedProject,
        oldProjectStatus,
        savedProject.getResearchers(),
        savedProject.getResearchers());

    List<Notification> notifications =
        collectNotifications(
            savedProject,
            savedProject.getStatus(),
            oldProjectStatus,
            savedProject.getCoordinator().getUserId(),
            savedProject.getResearchers(),
            savedProject.getResearchers(),
            user.getUserId());

    notificationService.send(notifications);

    return savedProject;
  }

  private Project updateProjectAllFields(
      ProjectDto projectDto, List<String> roles, UserDetails user, Project projectToEdit) {

    ProjectStatus oldStatus = projectToEdit.getStatus();

    validateCoordinatorIsOwner(projectToEdit, user.getUserId());
    validateStatus(projectToEdit.getStatus(), projectDto.getStatus(), roles);

    List<UserDetails> newResearchers = getResearchers(projectDto);
    List<UserDetails> oldResearchers = projectToEdit.getResearchers();
    projectToEdit.setResearchers(newResearchers);

    persistTransition(projectToEdit, projectToEdit.getStatus(), projectDto.getStatus(), user);

    if (ProjectStatus.APPROVED.equals(projectToEdit.getStatus())
        || ProjectStatus.PUBLISHED.equals(projectToEdit.getStatus())) {
      projectToEdit.setStatus(projectDto.getStatus());
      var savedProject = projectRepository.save(projectToEdit);

      registerToZarsIfNecessary(savedProject, oldStatus, oldResearchers, newResearchers);

      List<Notification> notifications =
          collectNotifications(
              savedProject,
              savedProject.getStatus(),
              oldStatus,
              savedProject.getCoordinator().getUserId(),
              newResearchers,
              oldResearchers,
              user.getUserId());

      notificationService.send(notifications);
      return savedProject;
    }
    setTemplates(projectToEdit, projectDto);

    projectToEdit.setStatus(projectDto.getStatus());
    projectToEdit.setName(projectDto.getName());
    projectToEdit.setSimpleDescription(projectDto.getSimpleDescription());
    projectToEdit.setUsedOutsideEu(projectDto.isUsedOutsideEu());
    projectToEdit.setDescription(projectDto.getDescription());
    projectToEdit.setModifiedDate(OffsetDateTime.now());
    projectToEdit.setFirstHypotheses(projectDto.getFirstHypotheses());
    projectToEdit.setSecondHypotheses(projectDto.getSecondHypotheses());
    projectToEdit.setGoal(projectDto.getGoal());
    projectToEdit.setCategories(projectDto.getCategories());
    projectToEdit.setKeywords(projectDto.getKeywords());
    projectToEdit.setStartDate(projectDto.getStartDate());
    projectToEdit.setEndDate(projectDto.getEndDate());
    projectToEdit.setFinanced(projectDto.isFinanced());

    var savedProject = projectRepository.save(projectToEdit);
    registerToZarsIfNecessary(savedProject, oldStatus, oldResearchers, newResearchers);

    List<Notification> notifications =
        collectNotifications(
            savedProject,
            savedProject.getStatus(),
            oldStatus,
            savedProject.getCoordinator().getUserId(),
            newResearchers,
            oldResearchers,
            user.getUserId());

    notificationService.send(notifications);

    return savedProject;
  }

  private List<Policy> collectProjectPolicies(
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

  private List<Notification> collectNotifications(
      Project project,
      ProjectStatus newStatus,
      ProjectStatus oldStatus,
      String coordinatorUserId,
      List<UserDetails> newResearchers,
      List<UserDetails> currentResearchers,
      String approverUserId) {

    List<Notification> notifications = new LinkedList<>();
    var coordinator = userService.getUserById(coordinatorUserId, false);

    if (isTransitionToPending(oldStatus, newStatus)) {

      Set<User> approvers = userService.getByRole(Roles.STUDY_APPROVER);

      approvers.forEach(
          approver -> {
            ProjectApprovalRequestNotification notification =
                ProjectApprovalRequestNotification.builder()
                    .coordinatorFirstName(coordinator.getFirstName())
                    .coordinatorLastName(coordinator.getLastName())
                    .projectTitle(project.getName())
                    .recipientEmail(approver.getEmail())
                    .recipientFirstName(approver.getFirstName())
                    .recipientLastName(approver.getLastName())
                    .projectId(project.getId())
                    .build();
            notifications.add(notification);
          });
    }

    if (isTransitionMadeByApprover(oldStatus, newStatus)) {

      var approver = userService.getUserById(approverUserId, false);

      ProjectStatusChangeNotification notification =
          ProjectStatusChangeNotification.builder()
              .recipientFirstName(coordinator.getFirstName())
              .recipientLastName(coordinator.getLastName())
              .recipientEmail(coordinator.getEmail())
              .projectTitle(project.getName())
              .projectStatus(newStatus)
              .approverFirstName(approver.getFirstName())
              .approverLastName(approver.getLastName())
              .projectId(project.getId())
              .oldProjectStatus(oldStatus)
              .build();
      notifications.add(notification);
    }

    if (isTransitionToPublished(oldStatus, newStatus) && newResearchers != null) {
      List<String> researcherIds =
          newResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());

      createProjectStartedNotifications(project, notifications, coordinator, researcherIds);
    }

    if (isTransitionToPublishedFromPublished(oldStatus, newStatus)) {
      List<String> newResearcherIds = new LinkedList<>();
      List<String> currentResearchersIds = new LinkedList<>();
      if (newResearchers != null) {
        newResearcherIds =
            newResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());
      }

      if (currentResearchers != null) {
        currentResearchersIds =
            currentResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());
      }

      var addedResearchersIds = new ArrayList<>(newResearcherIds);
      addedResearchersIds.removeAll(currentResearchersIds);

      currentResearchersIds.removeAll(newResearcherIds);

      createProjectStartedNotifications(project, notifications, coordinator, addedResearchersIds);
      createProjectClosedNotifications(
          project.getName(), notifications, coordinator, currentResearchersIds);
    }

    if (ProjectStatus.CLOSED.equals(newStatus)) {
      List<String> researcherIds = new LinkedList<>();
      if (currentResearchers != null) {
        researcherIds =
            currentResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());
      }
      createProjectClosedNotifications(
          project.getName(), notifications, coordinator, researcherIds);
    }

    return notifications;
  }

  private void createProjectClosedNotifications(
      String projectName,
      List<Notification> notifications,
      User coordinator,
      List<String> researcherIds) {
    researcherIds.forEach(
        r -> {
          var researcher = userService.getUserById(r, false);
          ProjectCloseNotification notification =
              ProjectCloseNotification.builder()
                  .recipientEmail(researcher.getEmail())
                  .recipientFirstName(researcher.getFirstName())
                  .recipientLastName(researcher.getLastName())
                  .coordinatorFirstName(coordinator.getFirstName())
                  .coordinatorLastName(coordinator.getLastName())
                  .projectTitle(projectName)
                  .build();
          notifications.add(notification);
        });
  }

  private void createProjectStartedNotifications(
      Project project,
      List<Notification> notifications,
      User coordinator,
      List<String> researcherIds) {
    researcherIds.forEach(
        r -> {
          var researcher = userService.getUserById(r, false);
          ProjectStartNotification notification =
              ProjectStartNotification.builder()
                  .recipientEmail(researcher.getEmail())
                  .recipientFirstName(researcher.getFirstName())
                  .recipientLastName(researcher.getLastName())
                  .coordinatorFirstName(coordinator.getFirstName())
                  .coordinatorLastName(coordinator.getLastName())
                  .projectTitle(project.getName())
                  .projectId(project.getId())
                  .build();
          notifications.add(notification);
        });
  }

  private boolean isTransitionToPending(ProjectStatus oldStatus, ProjectStatus newStatus) {
    return ProjectStatus.PENDING.equals(newStatus) && !newStatus.equals(oldStatus);
  }

  private boolean isTransitionMadeByApprover(ProjectStatus oldStatus, ProjectStatus newStatus) {
    return (ProjectStatus.APPROVED.equals(newStatus)
        || ProjectStatus.DENIED.equals(newStatus)
        || ProjectStatus.CHANGE_REQUEST.equals(newStatus)
        || ProjectStatus.REVIEWING.equals(newStatus))
        && !newStatus.equals(oldStatus);
  }

  private boolean isTransitionToPublished(ProjectStatus oldStatus, ProjectStatus newStatus) {
    return ProjectStatus.PUBLISHED.equals(newStatus) && !newStatus.equals(oldStatus);
  }

  private boolean isTransitionToPublishedFromPublished(
      ProjectStatus oldStatus, ProjectStatus newStatus) {
    return ProjectStatus.PUBLISHED.equals(oldStatus) && ProjectStatus.PUBLISHED.equals(newStatus);
  }

  private void registerToZarsIfNecessary(
      Project project,
      ProjectStatus oldStatus,
      List<UserDetails> oldResearchers,
      List<UserDetails> newResearchers) {
    ProjectStatus newStatus = project.getStatus();
    if (((newStatus == ProjectStatus.PENDING
        || newStatus == ProjectStatus.APPROVED
        || newStatus == ProjectStatus.PUBLISHED
        || newStatus == ProjectStatus.CLOSED)
        && newStatus != oldStatus)
        || (newStatus == ProjectStatus.PUBLISHED
        && researchersAreDifferent(oldResearchers, newResearchers))) {
      registerToZars(project);
    }
  }

  private boolean researchersAreDifferent(
      List<UserDetails> oldResearchers, List<UserDetails> newResearchers) {
    return !(oldResearchers.containsAll(newResearchers)
        && newResearchers.containsAll(oldResearchers));
  }

  public List<Project> getProjects(String userId, List<String> roles) {

    List<Project> projects = new ArrayList<>();

    if (roles.contains(Roles.STUDY_COORDINATOR)) {
      projects.addAll(
          projectRepository.findByCoordinatorUserIdOrStatusIn(
              userId,
              new ProjectStatus[] {
                  ProjectStatus.APPROVED, ProjectStatus.PUBLISHED, ProjectStatus.CLOSED
              }));
    }
    if (roles.contains(Roles.RESEARCHER)) {
      projects.addAll(
          projectRepository.findByResearchers_UserIdAndStatusIn(
              userId, new ProjectStatus[] {ProjectStatus.PUBLISHED, ProjectStatus.CLOSED}));
    }
    if (roles.contains(Roles.STUDY_APPROVER)) {
      ProjectStatus[] statuses =
          Stream.of(ProjectStatus.values())
              .filter(
                  projectStatus ->
                      projectStatus != ProjectStatus.DRAFT
                          && projectStatus != ProjectStatus.ARCHIVED)
              .collect(Collectors.toList())
              .toArray(new ProjectStatus[] {});
      projects.addAll(projectRepository.findByStatusIn(statuses));
    }

    return projects.stream().distinct().collect(Collectors.toList());
  }

  public String getExportFilenameBody(Long projectId) {
    return String.format(
            "Project_%d_%s",
            projectId,
            LocalDateTime.now()
                .truncatedTo(ChronoUnit.MINUTES)
                .format(DateTimeFormatter.ISO_LOCAL_DATE))
        .replace('-', '_');
  }

  private void setTemplates(Project project, ProjectDto projectDto) {
    if (projectDto.getTemplates() != null) {
      Map<String, String> map =
          projectDto.getTemplates().stream()
              .collect(
                  Collectors.toMap(
                      TemplateInfoDto::getTemplateId, TemplateInfoDto::getName, (t1, t2) -> t1));

      project.setTemplates(map);
    }
  }

  private List<UserDetails> getResearchers(ProjectDto projectDto) {
    List<UserDetails> newResearchersList = new LinkedList<>();

    if (projectDto.getResearchers() != null) {
      for (UserDetailsDto dto : projectDto.getResearchers()) {
        Optional<UserDetails> researcher = userDetailsService.getUserDetailsById(dto.getUserId());

        if (researcher.isEmpty()) {
          throw new BadRequestException("Researcher not found.");
        }

        if (researcher.get().isNotApproved()) {
          throw new BadRequestException("Researcher not approved.");
        }

        newResearchersList.add(researcher.get());
      }
    }
    return newResearchersList;
  }

  private void validateStatus(
      ProjectStatus initialStatus, ProjectStatus nextStatus, List<String> roles) {

    if (nextStatus == null) {
      throw new BadRequestException("Invalid project status");
    }

    if (initialStatus == null) {
      if (!isValidInitialStatus(nextStatus)) {
        throw new BadRequestException("Invalid project status: " + nextStatus);
      }
    } else if (initialStatus.nextStatusesAndRoles().containsKey(nextStatus)) {
      List<String> allowedRoles = initialStatus.nextStatusesAndRoles().get(nextStatus);

      Set<String> intersectionSet =
          roles.stream().distinct().filter(allowedRoles::contains).collect(Collectors.toSet());

      if (intersectionSet.isEmpty()) {
        throw new ForbiddenException(
            "Project status transition from "
                + initialStatus
                + " to "
                + nextStatus
                + " not allowed");
      }
    } else {
      throw new BadRequestException(
          "Project status transition from " + initialStatus + " to " + nextStatus + " not allowed");
    }
  }

  private boolean isValidInitialStatus(ProjectStatus status) {
    return status.equals(ProjectStatus.DRAFT) || status.equals(ProjectStatus.PENDING);
  }

  private void persistTransition(
      Project project, ProjectStatus fromStatus, ProjectStatus toStatus, UserDetails user) {

    if (fromStatus != null && fromStatus.equals(toStatus)) {
      return;
    }

    var projectTransition =
        ProjectTransition.builder()
            .toStatus(toStatus)
            .project(project)
            .user(user)
            .createDate(OffsetDateTime.now())
            .build();

    if (fromStatus != null) {
      projectTransition.setFromStatus(fromStatus);
    }

    if (project.getTransitions() != null) {
      project.getTransitions().add(projectTransition);
    } else {
      project.setTransitions(Set.of(projectTransition));
    }
  }

  private void validateCoordinatorIsOwner(Project project, String loggedInUser) {
    if (project.hasEmptyOrDifferentOwner(loggedInUser)) {
      throw new ForbiddenException("Cannot access this resource. User is not owner.");
    }
  }

  private ProjectInfoDto toProjectInfo(Project project, List<String> roles) {
    if (project == null) {
      return null;
    }

    var projectInfoDto =
        ProjectInfoDto.builder()
            .createDate(project.getCreateDate())
            .title(project.getName())
            .build();

    if (project.getCoordinator() != null) {
      if (roles.contains(Roles.RESEARCHER)
          || roles.contains(Roles.STUDY_COORDINATOR)
          || roles.contains(Roles.STUDY_APPROVER)) {

        var coordinator = userService.getUserById(project.getCoordinator().getUserId(), false);
        projectInfoDto.setCoordinator(
            String.format("%s %s", coordinator.getFirstName(), coordinator.getLastName()));
      } else {
        projectInfoDto.setCoordinator(StringUtils.EMPTY);
      }
      if (project.getCoordinator().getOrganization() != null) {
        projectInfoDto.setOrganization(project.getCoordinator().getOrganization().getName());
      }
    }
    return projectInfoDto;
  }

  private void registerToZars(Project project) {
    if (zarsService != null) {
      var zarsInfoDto = modelMapper.map(project, ZarsInfoDto.class);
      zarsInfoDto.setCoordinator(getCoordinator(project));
      zarsInfoDto.setQueries(getQueries(project));
      zarsInfoDto.setApprovalDate(getApprovalDateIfExists(project));
      zarsInfoDto.setPartners(getPartners(project));
      zarsInfoDto.setClosedDate(getClosedDateIfExists(project));
      zarsService.registerToZars(zarsInfoDto);
    } else {
      log.error(
          "Project change should be registered to ZARS, but necessary info is not configured. Not registered!");
    }
  }

  @NotNull
  private String getCoordinator(@NotNull Project project) {
    return userService.getUserById(project.getCoordinator().getUserId(), false).getUsername();
  }

  @NotNull
  private String getQueries(Project project) {
    if (project.getCohort() == null) {
      return StringUtils.EMPTY;
    }
    return String.join(", ", cohortQueryLister.list(project.getCohort()));
  }

  @NotNull
  private String getApprovalDateIfExists(Project project) {
    List<ProjectTransition> transitions =
        projectTransitionRepository
            .findAllByProjectIdAndFromStatusAndToStatus(
                project.getId(), ProjectStatus.REVIEWING, ProjectStatus.APPROVED)
            .orElse(Collections.emptyList());
    if (transitions.size() > 1) {
      log.error(
          "More than one transition from REVIEWING to APPROVED for project " + project.getId());
      return StringUtils.EMPTY;
    }
    if (transitions.size() == 1) {
      return transitions.get(0).getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    return StringUtils.EMPTY;
  }

  @NotNull
  private String getPartners(Project project) {
    Set<Organization> organizations = new HashSet<>();

    if (project.getCoordinator().getOrganization() != null) {
      organizations.add(project.getCoordinator().getOrganization());
    }
    project
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
  private String getClosedDateIfExists(Project project) {
    List<ProjectTransition> transitions =
        projectTransitionRepository
            .findAllByProjectIdAndFromStatusAndToStatus(
                project.getId(), ProjectStatus.PUBLISHED, ProjectStatus.CLOSED)
            .orElse(Collections.emptyList());
    if (transitions.size() > 1) {
      throw new SystemException(
          "More than one transition from PUBLISHED to CLOSED for project " + project.getId());
    }
    if (transitions.size() == 1) {
      return transitions.get(0).getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    return StringUtils.EMPTY;
  }

  private Project validateAndRetrieveProject(Long projectId, String userId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFound(PROJECT_NOT_FOUND + projectId));

    if (project.getStatus() == null || !project.getStatus().equals(ProjectStatus.PUBLISHED)) {
      throw new ForbiddenException("Data explorer available for published projects only");
    }

    if (!project.isProjectResearcher(userId) && project.hasEmptyOrDifferentOwner(userId)) {
      throw new ForbiddenException("Cannot access this project");
    }

    if (project.getCohort() == null) {
      throw new BadRequestException(
          String.format("Project: %s cohort cannot be null", project.getId()));
    }

    if (project.getTemplates() == null) {
      throw new BadRequestException(
          String.format("Project: %s templates cannot be null", project.getId()));
    }
    return project;
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
        .build();
  }

  public byte[] getInfoDocBytes(Long id, String userId, Locale locale) {
    userDetailsService.checkIsUserApproved(userId);
    Project project =
        projectRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  throw new BadRequestException("Project not found");
                });
    ProjectDto projectDto = projectMapper.convertToDto(project);
    try {
      return projectDocCreator.getDocBytesOfProject(projectDto, locale);
    } catch (IOException e) {
      throw new SystemException("Error creating the project PDF");
    }
  }
}
