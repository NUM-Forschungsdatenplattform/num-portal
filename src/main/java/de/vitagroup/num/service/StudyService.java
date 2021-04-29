package de.vitagroup.num.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.ExportType;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.StudyTransition;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.ProjectInfoDto;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.dto.ZarsInfoDto;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.StudyTransitionRepository;
import de.vitagroup.num.properties.ConsentProperties;
import de.vitagroup.num.service.atna.AtnaService;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.email.ZarsService;
import de.vitagroup.num.service.executors.CohortQueryLister;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.ProjectCloseNotification;
import de.vitagroup.num.service.notification.dto.ProjectRequestNotification;
import de.vitagroup.num.service.notification.dto.ProjectStartNotification;
import de.vitagroup.num.service.notification.dto.ProjectStatusChangeNotification;
import de.vitagroup.num.service.policy.EhrPolicy;
import de.vitagroup.num.service.policy.EuropeanConsentPolicy;
import de.vitagroup.num.service.policy.Policy;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import de.vitagroup.num.service.policy.TemplatesPolicy;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
public class StudyService {

  private static final String STUDY_NOT_FOUND = "Study not found: ";
  private static final String ZIP_FILE_ENDING = ".zip";
  private static final String JSON_FILE_ENDING = ".json";
  private static final String ZIP_MEDIA_TYPE = "application/zip";
  private static final String CSV_FILE_PATTERN = "%s_%d.csv";

  private final StudyRepository studyRepository;

  private final UserDetailsService userDetailsService;

  private final EhrBaseService ehrBaseService;

  private final ObjectMapper mapper;

  private final AtnaService atnaService;

  private final UserService userService;

  private final NotificationService notificationService;

  private final StudyTransitionRepository studyTransitionRepository;

  private final CohortQueryLister cohortQueryLister;

  private final ModelMapper modelMapper;

  @Nullable private final ZarsService zarsService;

  private final ProjectPolicyService projectPolicyService;

  private final CohortService cohortService;

  private final ConsentProperties consentProperties;

  public void deleteProject(Long projectId, String userId, List<String> roles) {
    userDetailsService.checkIsUserApproved(userId);

    Study project =
        studyRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFound(STUDY_NOT_FOUND + projectId));

    if (project.hasEmptyOrDifferentOwner(userId) && !roles.contains(Roles.SUPER_ADMIN)) {
      throw new ForbiddenException(String.format("Cannot delete project: %s", projectId));
    }

    if (project.isDeletable()) {
      studyRepository.deleteById(projectId);
    } else {
      throw new ForbiddenException(
          String.format(
              "Cannot delete project: %s, invalid status: %s", projectId, project.getStatus()));
    }
  }

  @Transactional
  public void archiveProject(Long projectId, String userId, List<String> roles) {
    UserDetails user = userDetailsService.checkIsUserApproved(userId);

    Study project =
        studyRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFound(STUDY_NOT_FOUND + projectId));

    if (project.hasEmptyOrDifferentOwner(userId) && !roles.contains(Roles.SUPER_ADMIN)) {
      throw new ForbiddenException(String.format("Cannot archive project: %s", projectId));
    }

    validateStatus(project.getStatus(), StudyStatus.ARCHIVED, roles);
    persistTransition(project, project.getStatus(), StudyStatus.ARCHIVED, user);

    project.setStatus(StudyStatus.ARCHIVED);
    studyRepository.save(project);
  }

  /**
   * Counts the number of projects existing in the platform
   *
   * @return The count of projects in the platform
   */
  public long countProjects() {
    return studyRepository.count();
  }

  /**
   * Retrieves a list of latest projects information
   *
   * @param count number of projects to be retrieved
   * @return The list of max requested count of latest projects
   */
  public List<ProjectInfoDto> getLatestProjectsInfo(int count) {

    if (count < 1) {
      return List.of();
    }

    List<Study> projects =
        studyRepository.findLatestProjects(
            count,
            StudyStatus.APPROVED.name(),
            StudyStatus.PUBLISHED.name(),
            StudyStatus.CLOSED.name());
    return projects.stream().map(this::toProjectInfo).collect(Collectors.toList());
  }

  public String executeAqlAndJsonify(String query, Long studyId, String userId) {
    List<QueryResponseData> response = executeAql(query, studyId, userId);
    try {
      return mapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new SystemException("An issue has occurred, cannot execute aql.");
    }
  }

  public List<QueryResponseData> executeAql(String query, Long studyId, String userId) {
    List<QueryResponseData> queryResponseData;
    Study study = null;
    try {
      userDetailsService.checkIsUserApproved(userId);

      study =
          studyRepository
              .findById(studyId)
              .orElseThrow(() -> new ResourceNotFound(STUDY_NOT_FOUND + studyId));

      if (!study.isStudyResearcher(userId) && study.hasEmptyOrDifferentOwner(userId)) {
        throw new ForbiddenException("Cannot access this study");
      }

      if (study.getCohort() == null) {
        throw new BadRequestException(String.format("Study: %s cohort cannot be null", studyId));
      }

      if (study.getTemplates() == null) {
        throw new BadRequestException(String.format("Study: %s templates cannot be null", studyId));
      }

      Set<String> ehrIds =
          cohortService.executeCohort(study.getCohort().getId(), study.isUsedOutsideEu());

      AqlDto aql = new AqlToDtoParser().parse(query);

      List<Policy> policies =
          collectProjectPolicies(ehrIds, study.getTemplates(), study.isUsedOutsideEu());
      projectPolicyService.apply(aql, policies);

      queryResponseData = ehrBaseService.executeRawQuery(aql, studyId);

    } catch (Exception e) {
      atnaService.logDataExport(userId, studyId, study, false);
      throw e;
    }
    atnaService.logDataExport(userId, studyId, study, true);
    return queryResponseData;
  }

  public void streamResponseAsZip(
      List<QueryResponseData> queryResponseDataList,
      String filenameStart,
      OutputStream outputStream) {

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

      int index = 0;
      for (QueryResponseData queryResponseData : queryResponseDataList) {

        zipOutputStream.putNextEntry(
            new ZipEntry(String.format(CSV_FILE_PATTERN, filenameStart, index)));
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
              .print(new OutputStreamWriter(zipOutputStream));

      for (List<Object> row : queryResponseData.getRows()) {
        printer.printRecord(row);
      }
      printer.flush();
    } catch (IOException e) {
      throw new SystemException("Error while creating the CSV file");
    }
  }

  public StreamingResponseBody getExportResponseBody(
      String query, Long studyId, String userId, ExportType format) {
    if (format == ExportType.json) {
      String jsonResponse = executeAqlAndJsonify(query, studyId, userId);
      return outputStream -> {
        outputStream.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
      };
    }
    List<QueryResponseData> queryResponseData = executeAql(query, studyId, userId);

    return outputStream ->
        streamResponseAsZip(queryResponseData, getExportFilenameBody(studyId), outputStream);
  }

  public MultiValueMap<String, String> getExportHeaders(ExportType format, Long studyId) {
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
        "attachment; filename=" + getExportFilenameBody(studyId) + fileEnding);
    return headers;
  }

  public Optional<Study> getStudyById(Long studyId) {
    return studyRepository.findById(studyId);
  }

  public boolean exists(Long studyId) {
    return studyRepository.existsById(studyId);
  }

  @Transactional
  public Study createStudy(StudyDto studyDto, String userId, List<String> roles) {

    UserDetails coordinator = userDetailsService.checkIsUserApproved(userId);

    Study study = Study.builder().build();

    validateStatus(null, studyDto.getStatus(), roles);
    persistTransition(study, study.getStatus(), studyDto.getStatus(), coordinator);

    setTemplates(study, studyDto);
    study.setResearchers(getResearchers(studyDto));

    study.setStatus(studyDto.getStatus());
    study.setName(studyDto.getName());
    study.setDescription(studyDto.getDescription());
    study.setSimpleDescription(studyDto.getSimpleDescription());
    study.setUsedOutsideEu(studyDto.isUsedOutsideEu());
    study.setFirstHypotheses(studyDto.getFirstHypotheses());
    study.setSecondHypotheses(studyDto.getSecondHypotheses());
    study.setGoal(studyDto.getGoal());
    study.setCategories(studyDto.getCategories());
    study.setKeywords(studyDto.getKeywords());
    study.setCoordinator(coordinator);
    study.setCreateDate(OffsetDateTime.now());
    study.setModifiedDate(OffsetDateTime.now());
    study.setStartDate(studyDto.getStartDate());
    study.setEndDate(studyDto.getEndDate());
    study.setFinanced(studyDto.isFinanced());

    Study savedStudy = studyRepository.save(study);

    if (savedStudy.getStatus() == StudyStatus.PENDING) {
      registerToZars(study);
    }

    List<Notification> notifications =
        collectNotifications(
            savedStudy.getName(),
            savedStudy.getStatus(),
            null,
            savedStudy.getCoordinator().getUserId(),
            savedStudy.getResearchers(),
            savedStudy.getResearchers(),
            userId);

    notificationService.send(notifications);

    return savedStudy;
  }

  @Transactional
  public Study updateStudy(StudyDto studyDto, Long id, String userId, List<String> roles) {
    UserDetails user = userDetailsService.checkIsUserApproved(userId);

    Study studyToEdit =
        studyRepository.findById(id).orElseThrow(() -> new ResourceNotFound(STUDY_NOT_FOUND + id));

    if (StudyStatus.ARCHIVED.equals(studyToEdit.getStatus())
        || StudyStatus.CLOSED.equals(studyToEdit.getStatus())) {
      throw new ForbiddenException(
          String.format(
              "Cannot update study: %s, invalid study status: %s", id, studyToEdit.getStatus()));
    }

    if (CollectionUtils.isNotEmpty(roles)
        && roles.contains(Roles.STUDY_COORDINATOR)
        && studyToEdit.isCoordinator(userId)) {
      return updateStudyAllFields(studyDto, roles, user, studyToEdit);
    } else if (CollectionUtils.isNotEmpty(roles) && roles.contains(Roles.STUDY_APPROVER)) {
      return updateStudyStatus(studyDto, roles, user, studyToEdit);
    } else {
      throw new ForbiddenException("No permissions to edit this study");
    }
  }

  private Study updateStudyStatus(
      StudyDto studyDto, List<String> roles, UserDetails user, Study studyToEdit) {

    StudyStatus oldStudyStatus = studyToEdit.getStatus();

    validateStatus(studyToEdit.getStatus(), studyDto.getStatus(), roles);
    persistTransition(studyToEdit, studyToEdit.getStatus(), studyDto.getStatus(), user);
    studyToEdit.setStatus(studyDto.getStatus());

    Study savedStudy = studyRepository.save(studyToEdit);

    registerToZarsIfNecessary(
        savedStudy, oldStudyStatus, savedStudy.getResearchers(), savedStudy.getResearchers());

    List<Notification> notifications =
        collectNotifications(
            savedStudy.getName(),
            savedStudy.getStatus(),
            oldStudyStatus,
            savedStudy.getCoordinator().getUserId(),
            savedStudy.getResearchers(),
            savedStudy.getResearchers(),
            user.getUserId());

    notificationService.send(notifications);

    return savedStudy;
  }

  private Study updateStudyAllFields(
      StudyDto studyDto, List<String> roles, UserDetails user, Study studyToEdit) {

    StudyStatus oldStatus = studyToEdit.getStatus();

    validateCoordinatorIsOwner(studyToEdit, user.getUserId());
    validateStatus(studyToEdit.getStatus(), studyDto.getStatus(), roles);

    List<UserDetails> newResearchers = getResearchers(studyDto);
    List<UserDetails> oldResearchers = studyToEdit.getResearchers();
    studyToEdit.setResearchers(newResearchers);

    persistTransition(studyToEdit, studyToEdit.getStatus(), studyDto.getStatus(), user);

    if (StudyStatus.APPROVED.equals(studyToEdit.getStatus())
        || StudyStatus.PUBLISHED.equals(studyToEdit.getStatus())) {
      studyToEdit.setStatus(studyDto.getStatus());
      Study savedStudy = studyRepository.save(studyToEdit);

      registerToZarsIfNecessary(savedStudy, oldStatus, oldResearchers, newResearchers);

      List<Notification> notifications =
          collectNotifications(
              savedStudy.getName(),
              savedStudy.getStatus(),
              oldStatus,
              savedStudy.getCoordinator().getUserId(),
              newResearchers,
              oldResearchers,
              user.getUserId());

      notificationService.send(notifications);
      return savedStudy;
    }
    setTemplates(studyToEdit, studyDto);

    studyToEdit.setStatus(studyDto.getStatus());
    studyToEdit.setName(studyDto.getName());
    studyToEdit.setSimpleDescription(studyDto.getSimpleDescription());
    studyToEdit.setUsedOutsideEu(studyDto.isUsedOutsideEu());
    studyToEdit.setDescription(studyDto.getDescription());
    studyToEdit.setModifiedDate(OffsetDateTime.now());
    studyToEdit.setFirstHypotheses(studyDto.getFirstHypotheses());
    studyToEdit.setSecondHypotheses(studyDto.getSecondHypotheses());
    studyToEdit.setGoal(studyDto.getGoal());
    studyToEdit.setCategories(studyDto.getCategories());
    studyToEdit.setKeywords(studyDto.getKeywords());
    studyToEdit.setStartDate(studyDto.getStartDate());
    studyToEdit.setEndDate(studyDto.getEndDate());
    studyToEdit.setFinanced(studyDto.isFinanced());

    Study savedStudy = studyRepository.save(studyToEdit);
    registerToZarsIfNecessary(savedStudy, oldStatus, oldResearchers, newResearchers);

    List<Notification> notifications =
        collectNotifications(
            savedStudy.getName(),
            savedStudy.getStatus(),
            oldStatus,
            savedStudy.getCoordinator().getUserId(),
            newResearchers,
            oldResearchers,
            user.getUserId());

    notificationService.send(notifications);

    return savedStudy;
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
      String projectName,
      StudyStatus newStatus,
      StudyStatus oldStatus,
      String coordinatorUserId,
      List<UserDetails> newResearchers,
      List<UserDetails> oldResearchers,
      String approverUserId) {

    List<Notification> notifications = new LinkedList<>();
    User coordinator = userService.getUserById(coordinatorUserId, false);

    if (isTransitionToPending(oldStatus, newStatus)) {

      Set<User> approvers = userService.getByRole(Roles.STUDY_APPROVER);

      approvers.forEach(
          approver -> {
            ProjectRequestNotification notification =
                ProjectRequestNotification.builder()
                    .coordinatorFirstName(coordinator.getFirstName())
                    .coordinatorLastName(coordinator.getLastName())
                    .projectTitle(projectName)
                    .recipientEmail(approver.getEmail())
                    .recipientFirstName(approver.getFirstName())
                    .recipientLastName(approver.getLastName())
                    .build();
            notifications.add(notification);
          });
    }

    if (isTransitionMadeByApprover(oldStatus, newStatus)) {

      User approver = userService.getUserById(approverUserId, false);

      ProjectStatusChangeNotification notification =
          ProjectStatusChangeNotification.builder()
              .recipientFirstName(coordinator.getFirstName())
              .recipientLastName(coordinator.getLastName())
              .recipientEmail(coordinator.getEmail())
              .projectTitle(projectName)
              .projectStatus(newStatus)
              .approverFirstName(approver.getFirstName())
              .approverLastName(approver.getLastName())
              .build();
      notifications.add(notification);
    }

    if (isTransitionToPublished(oldStatus, newStatus)) {
      if (newResearchers != null) {
        List<String> researcherIds =
            newResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());

        researcherIds.forEach(
            r -> {
              User researcher = userService.getUserById(r, false);
              ProjectStartNotification notification =
                  ProjectStartNotification.builder()
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
    }

    if (isTransitionToPublishedFromPublished(oldStatus, newStatus)) {
      List<String> newResearcherIds = new LinkedList<>();
      List<String> oldResearcherIds = new LinkedList<>();
      if (newResearchers != null) {
        newResearcherIds =
            newResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());
      }

      if (oldResearchers != null) {
        oldResearcherIds =
            oldResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());
      }

      List<String> newResearcherIdsCopy = new ArrayList<>(newResearcherIds);

      newResearcherIdsCopy.removeAll(oldResearcherIds);
      oldResearcherIds.removeAll(newResearcherIds);

      newResearcherIdsCopy.forEach(
          r -> {
            User researcher = userService.getUserById(r, false);
            ProjectStartNotification notification =
                ProjectStartNotification.builder()
                    .recipientEmail(researcher.getEmail())
                    .recipientFirstName(researcher.getFirstName())
                    .recipientLastName(researcher.getLastName())
                    .coordinatorFirstName(coordinator.getFirstName())
                    .coordinatorLastName(coordinator.getLastName())
                    .projectTitle(projectName)
                    .build();
            notifications.add(notification);
          });

      oldResearcherIds.forEach(
          r -> {
            User researcher = userService.getUserById(r, false);
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

    if (StudyStatus.CLOSED.equals(newStatus)) {
      List<String> researcherIds = new LinkedList<>();
      if (oldResearchers != null) {
        researcherIds =
            oldResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());
      }
      researcherIds.forEach(
          r -> {
            User researcher = userService.getUserById(r, false);
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

    return notifications;
  }

  private boolean isTransitionToPending(StudyStatus oldStatus, StudyStatus newStatus) {
    return StudyStatus.PENDING.equals(newStatus) && !newStatus.equals(oldStatus);
  }

  private boolean isTransitionMadeByApprover(StudyStatus oldStatus, StudyStatus newStatus) {
    return (StudyStatus.APPROVED.equals(newStatus)
            || StudyStatus.DENIED.equals(newStatus)
            || StudyStatus.CHANGE_REQUEST.equals(newStatus)
            || StudyStatus.REVIEWING.equals(newStatus))
        && !newStatus.equals(oldStatus);
  }

  private boolean isTransitionToPublished(StudyStatus oldStatus, StudyStatus newStatus) {
    return StudyStatus.PUBLISHED.equals(newStatus) && !newStatus.equals(oldStatus);
  }

  private boolean isTransitionToPublishedFromPublished(
      StudyStatus oldStatus, StudyStatus newStatus) {
    return StudyStatus.PUBLISHED.equals(oldStatus) && StudyStatus.PUBLISHED.equals(newStatus);
  }

  private void registerToZarsIfNecessary(
      Study study,
      StudyStatus oldStatus,
      List<UserDetails> oldResearchers,
      List<UserDetails> newResearchers) {
    StudyStatus newStatus = study.getStatus();
    if (((newStatus == StudyStatus.PENDING
                || newStatus == StudyStatus.APPROVED
                || newStatus == StudyStatus.PUBLISHED
                || newStatus == StudyStatus.CLOSED)
            && newStatus != oldStatus)
        || (newStatus == StudyStatus.PUBLISHED
            && researchersAreDifferent(oldResearchers, newResearchers))) {
      registerToZars(study);
    }
  }

  private boolean researchersAreDifferent(
      List<UserDetails> oldResearchers, List<UserDetails> newResearchers) {
    return !(oldResearchers.containsAll(newResearchers)
        && newResearchers.containsAll(oldResearchers));
  }

  public List<Study> getStudies(String userId, List<String> roles) {

    List<Study> studiesList = new ArrayList<>();

    if (roles.contains(Roles.STUDY_COORDINATOR)) {
      studiesList.addAll(studyRepository.findByCoordinatorUserId(userId));
    }
    if (roles.contains(Roles.RESEARCHER)) {
      studiesList.addAll(
          studyRepository.findByResearchers_UserIdAndStatusIn(
              userId, new StudyStatus[] {StudyStatus.PUBLISHED, StudyStatus.CLOSED}));
    }
    if (roles.contains(Roles.STUDY_APPROVER)) {
      studiesList.addAll(
          studyRepository.findByStatusIn(
              new StudyStatus[] {StudyStatus.PENDING, StudyStatus.REVIEWING}));
    }

    return studiesList.stream().distinct().collect(Collectors.toList());
  }

  public String getExportFilenameBody(Long studyId) {
    return String.format(
            "Project_%d_%s",
            studyId,
            LocalDateTime.now()
                .truncatedTo(ChronoUnit.MINUTES)
                .format(DateTimeFormatter.ISO_LOCAL_DATE))
        .replace('-', '_');
  }

  private void setTemplates(Study study, StudyDto studyDto) {
    if (studyDto.getTemplates() != null) {
      Map<String, String> map =
          studyDto.getTemplates().stream()
              .collect(
                  Collectors.toMap(
                      TemplateInfoDto::getTemplateId, TemplateInfoDto::getName, (t1, t2) -> t1));

      study.setTemplates(map);
    }
  }

  private List<UserDetails> getResearchers(StudyDto studyDto) {
    List<UserDetails> newResearchersList = new LinkedList<>();

    if (studyDto.getResearchers() != null) {
      for (UserDetailsDto dto : studyDto.getResearchers()) {
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
      StudyStatus initialStatus, StudyStatus nextStatus, List<String> roles) {

    if (nextStatus == null) {
      throw new BadRequestException("Invalid study status");
    }

    if (initialStatus == null) {
      if (!isValidInitialStatus(nextStatus)) {
        throw new BadRequestException("Invalid study status: " + nextStatus);
      }
    } else if (initialStatus.nextStatusesAndRoles().containsKey(nextStatus)) {
      List<String> allowedRoles = initialStatus.nextStatusesAndRoles().get(nextStatus);

      Set<String> intersectionSet =
          roles.stream().distinct().filter(allowedRoles::contains).collect(Collectors.toSet());

      if (intersectionSet.isEmpty()) {
        throw new ForbiddenException(
            "Study status transition from " + initialStatus + " to " + nextStatus + " not allowed");
      }
    } else {
      throw new BadRequestException(
          "Study status transition from " + initialStatus + " to " + nextStatus + " not allowed");
    }
  }

  private boolean isValidInitialStatus(StudyStatus status) {
    return status.equals(StudyStatus.DRAFT) || status.equals(StudyStatus.PENDING);
  }

  private void persistTransition(
      Study study, StudyStatus fromStatus, StudyStatus toStatus, UserDetails user) {

    if (fromStatus != null && fromStatus.equals(toStatus)) {
      return;
    }

    StudyTransition studyTransition =
        StudyTransition.builder()
            .toStatus(toStatus)
            .study(study)
            .user(user)
            .createDate(OffsetDateTime.now())
            .build();

    if (fromStatus != null) {
      studyTransition.setFromStatus(fromStatus);
    }

    if (study.getTransitions() != null) {
      study.getTransitions().add(studyTransition);
    } else {
      study.setTransitions(Set.of(studyTransition));
    }
  }

  private void validateCoordinatorIsOwner(Study study, String loggedInUser) {
    if (study.hasEmptyOrDifferentOwner(loggedInUser)) {
      throw new ForbiddenException("Cannot access this resource. User is not owner.");
    }
  }

  private ProjectInfoDto toProjectInfo(Study study) {
    if (study == null) {
      return null;
    }

    ProjectInfoDto project =
        ProjectInfoDto.builder().createDate(study.getCreateDate()).title(study.getName()).build();

    if (study.getCoordinator() != null) {
      User coordinator = userService.getUserById(study.getCoordinator().getUserId(), false);
      project.setCoordinator(
          String.format("%s %s", coordinator.getFirstName(), coordinator.getLastName()));

      if (study.getCoordinator().getOrganization() != null) {
        project.setOrganization(study.getCoordinator().getOrganization().getName());
      }
    }
    return project;
  }

  private void registerToZars(Study study) {
    if (zarsService != null) {
      ZarsInfoDto zarsInfoDto = modelMapper.map(study, ZarsInfoDto.class);
      zarsInfoDto.setCoordinator(getCoordinator(study));
      zarsInfoDto.setQueries(getQueries(study));
      zarsInfoDto.setApprovalDate(getApprovalDateIfExists(study));
      zarsInfoDto.setPartners(getPartners(study));
      zarsInfoDto.setClosedDate(getClosedDateIfExists(study));
      zarsService.registerToZars(zarsInfoDto);
    } else {
      log.error(
          "Project change should be registered to ZARS, but necessary info is not configured. Not registered!");
    }
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
