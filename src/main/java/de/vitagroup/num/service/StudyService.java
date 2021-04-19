package de.vitagroup.num.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.ExportType;
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
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.service.atna.AtnaService;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.email.ZarsService;
import de.vitagroup.num.service.notification.NotificationService;
import de.vitagroup.num.service.notification.dto.Notification;
import de.vitagroup.num.service.notification.dto.ProjectCloseNotification;
import de.vitagroup.num.service.notification.dto.ProjectRequestNotification;
import de.vitagroup.num.service.notification.dto.ProjectStartNotification;
import de.vitagroup.num.service.notification.dto.ProjectStatusChangeNotification;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.ConditionDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorSymbol;
import org.ehrbase.aql.dto.condition.MatchesOperatorDto;
import org.ehrbase.aql.dto.condition.SimpleValue;
import org.ehrbase.aql.dto.condition.Value;
import org.ehrbase.aql.dto.containment.ContainmentDto;
import org.ehrbase.aql.dto.containment.ContainmentExpresionDto;
import org.ehrbase.aql.dto.containment.ContainmentLogicalOperator;
import org.ehrbase.aql.dto.containment.ContainmentLogicalOperatorSymbol;
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.ehrbase.response.openehr.QueryResponseData;
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

  private static final String EHR_ID_PATH = "/ehr_id/value";
  private static final String TEMPLATE_ID_PATH = "/archetype_details/template_id/value";
  private static final String COMPOSITION_ARCHETYPE_ID = "COMPOSITION";
  private static final String STUDY_NOT_FOUND = "Study not found: ";
  private static final String CSV_FILE_ENDING = ".zip";
  private static final String JSON_FILE_ENDING = ".json";
  private static final String ZIP_MEDIA_TYPE = "application/zip";
  private static final String CSV_FILE_PATTERN = "%s_%d.csv";

  private final StudyRepository studyRepository;

  private final UserDetailsService userDetailsService;

  private final EhrBaseService ehrBaseService;

  private final ObjectMapper mapper;

  private final CohortService cohortService;

  private final AtnaService atnaService;

  private final UserService userService;

  private final NotificationService notificationService;

  @Nullable private final ZarsService zarsService;

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
   * @return
   */
  public long countProjects() {
    return studyRepository.count();
  }

  /**
   * Retrieves a list of latest projects information
   *
   * @param count number of projects to be retrieved
   * @return
   */
  public List<ProjectInfoDto> getLatestProjectsInfo(int count) {

    if (count < 1) {
      return List.of();
    }

    List<Study> projects = studyRepository.findLatestProjects(count);
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

      String restrictedQuery = restrictQueryToStudy(query, study);

      queryResponseData = ehrBaseService.executeRawQuery(restrictedQuery);

    } catch (Exception e) {
      atnaService.logDataExport(userId, studyId, study, false);
      throw e;
    }
    atnaService.logDataExport(userId, studyId, study, true);
    return queryResponseData;
  }

  public void streamResponseAsCsv(
      List<QueryResponseData> queryResponseDataList,
      String filenameStart,
      OutputStream outputStream) {

    try {
      ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

      int index = 0;
      for (QueryResponseData queryResponseData : queryResponseDataList) {

        zipOutputStream.putNextEntry(new ZipEntry(String.format(CSV_FILE_PATTERN, filenameStart, index)));
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
        zipOutputStream.closeEntry();
        index++;
      }
      zipOutputStream.close();
    } catch (IOException e) {
      log.error("Error creating a zip file for data export.", e);
      throw new SystemException(
          "Error creating a zip file for data export: " + e.getLocalizedMessage());
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

    return outputStream -> streamResponseAsCsv(queryResponseData, getExportFilenameBody(studyId), outputStream);
  }

  public MultiValueMap<String, String> getExportHeaders(ExportType format, Long studyId) {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    String fileEnding;
    if (format == ExportType.json) {
      fileEnding = JSON_FILE_ENDING;
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    } else {
      fileEnding = CSV_FILE_ENDING;
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
        List<String> reasercherIds =
            newResearchers.stream().map(UserDetails::getUserId).collect(Collectors.toList());

        reasercherIds.forEach(
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

  private String restrictQueryToStudy(String query, Study study) {
    return restrictToStudyTemplates(restrictToCohortEhrIds(query, study), study.getTemplates());
  }

  private String restrictToStudyTemplates(String query, Map<String, String> templatesMap) {

    if (MapUtils.isEmpty(templatesMap)) {
      throw new BadRequestException("No templates attached to this study");
    }
    AqlDto aql = new AqlToDtoParser().parse(query);

    List<SelectFieldDto> whereClauseSelectFields = new LinkedList<>();

    ContainmentExpresionDto contains = aql.getContains();
    int nextContainmentId = findNextContainmentId(contains);

    if (contains != null) {
      List<Integer> compositions = findCompositions(contains);

      if (CollectionUtils.isNotEmpty(compositions)) {
        compositions.forEach(
            id -> {
              SelectFieldDto selectFieldDto = new SelectFieldDto();
              selectFieldDto.setAqlPath(TEMPLATE_ID_PATH);
              selectFieldDto.setContainmentId(id);
              whereClauseSelectFields.add(selectFieldDto);
            });

      } else {
        extendContainsClause(aql, whereClauseSelectFields, contains, nextContainmentId);
      }
    } else {
      createContainsClause(aql, whereClauseSelectFields, nextContainmentId);
    }

    List<Value> templateValues = toSimpleValueList(templatesMap.keySet());
    extendWhereClause(aql, whereClauseSelectFields, templateValues);

    return new AqlBinder().bind(aql).getLeft().buildAql();
  }

  private void createContainsClause(
      AqlDto aql, List<SelectFieldDto> whereClauseSelectFields, int nextContainmentId) {

    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(TEMPLATE_ID_PATH);
    select.setContainmentId(nextContainmentId);
    whereClauseSelectFields.add(select);

    ContainmentDto composition = new ContainmentDto();
    composition.setId(nextContainmentId);
    composition.setArchetypeId(COMPOSITION_ARCHETYPE_ID);
    aql.setContains(composition);
  }

  private void extendContainsClause(
      AqlDto aql,
      List<SelectFieldDto> whereClauseSelectFields,
      ContainmentExpresionDto contains,
      int nextContainmentId) {
    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(TEMPLATE_ID_PATH);
    select.setContainmentId(nextContainmentId);
    whereClauseSelectFields.add(select);

    ContainmentLogicalOperator newContains = new ContainmentLogicalOperator();
    newContains.setValues(new ArrayList<>());

    ContainmentDto composition = new ContainmentDto();
    composition.setId(nextContainmentId);
    composition.setArchetypeId(COMPOSITION_ARCHETYPE_ID);

    newContains.setSymbol(ContainmentLogicalOperatorSymbol.AND);
    newContains.getValues().add(composition);
    newContains.getValues().add(contains);

    aql.setContains(newContains);
  }

  private String restrictToCohortEhrIds(String query, Study study) {
    if (study.getCohort() == null) {
      throw new BadRequestException("Study cohort cannot be empty");
    }

    Set<String> ehrIds = cohortService.executeCohort(study.getCohort().getId());

    if (CollectionUtils.isEmpty(ehrIds)) {
      throw new BadRequestException("Cohort size cannot be 0");
    }

    AqlDto aql = new AqlToDtoParser().parse(query);

    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(EHR_ID_PATH);
    select.setContainmentId(aql.getEhr().getContainmentId());

    extendWhereClause(aql, List.of(select), toSimpleValueList(ehrIds));

    return new AqlBinder().bind(aql).getLeft().buildAql();
  }

  private void extendWhereClause(AqlDto aql, List<SelectFieldDto> selects, List<Value> values) {
    List<MatchesOperatorDto> matchesOperatorDtos = new LinkedList<>();

    selects.forEach(
        selectFieldDto -> {
          MatchesOperatorDto matches = new MatchesOperatorDto();
          matches.setStatement(selectFieldDto);
          matches.setValues(values);
          matchesOperatorDtos.add(matches);
        });

    ConditionLogicalOperatorDto newWhere = new ConditionLogicalOperatorDto();
    newWhere.setValues(new ArrayList<>());
    ConditionDto where = aql.getWhere();

    if (where != null) {
      newWhere.setSymbol(ConditionLogicalOperatorSymbol.AND);
      newWhere.getValues().add(where);
    }

    if (CollectionUtils.isNotEmpty(matchesOperatorDtos) && matchesOperatorDtos.size() > 1) {
      newWhere.setSymbol(ConditionLogicalOperatorSymbol.AND);
    }

    matchesOperatorDtos.forEach(
        matchesOperatorDto -> {
          newWhere.getValues().add(matchesOperatorDto);
        });

    aql.setWhere(newWhere);
  }

  private List<Integer> findCompositions(ContainmentExpresionDto dto) {
    if (dto == null) {
      return null;
    }

    List<Integer> compositions = new LinkedList<>();

    Queue<ContainmentExpresionDto> queue = new ArrayDeque<>();
    queue.add(dto);

    while (!queue.isEmpty()) {
      ContainmentExpresionDto current = queue.remove();

      if (current instanceof ContainmentLogicalOperator) {

        ContainmentLogicalOperator containmentLogicalOperator =
            (ContainmentLogicalOperator) current;

        queue.addAll(containmentLogicalOperator.getValues());

      } else if (current instanceof ContainmentDto) {

        ContainmentDto containmentDto = (ContainmentDto) current;

        if (containmentDto.getArchetypeId().contains(COMPOSITION_ARCHETYPE_ID)) {
          compositions.add(containmentDto.getId());
        }

        if (containmentDto.getContains() != null) {
          queue.add(containmentDto.getContains());
        }
      }
    }
    return compositions;
  }

  private Integer findNextContainmentId(ContainmentExpresionDto dto) {

    if (dto == null) {
      return 1;
    }

    Queue<ContainmentExpresionDto> queue = new ArrayDeque<>();
    queue.add(dto);

    int nextId = 0;

    while (!queue.isEmpty()) {
      ContainmentExpresionDto current = queue.remove();

      if (current instanceof ContainmentLogicalOperator) {

        ContainmentLogicalOperator containmentLogicalOperator =
            (ContainmentLogicalOperator) current;

        queue.addAll(containmentLogicalOperator.getValues());

      } else if (current instanceof ContainmentDto) {

        ContainmentDto containmentDto = (ContainmentDto) current;

        if (containmentDto.getId() > nextId) {
          nextId = containmentDto.getId();
        }

        if (containmentDto.getContains() != null) {
          queue.add(containmentDto.getContains());
        }
      }
    }
    return nextId + 1;
  }

  private List<Value> toSimpleValueList(Collection<String> list) {
    return list.stream()
        .map(
            s -> {
              SimpleValue simpleValue = new SimpleValue();
              simpleValue.setValue(s);
              return simpleValue;
            })
        .collect(Collectors.toList());
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
      zarsService.registerToZars(study);
    }
  }
}
