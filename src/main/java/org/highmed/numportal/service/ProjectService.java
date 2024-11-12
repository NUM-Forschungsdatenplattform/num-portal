package org.highmed.numportal.service;

import org.highmed.numportal.attachment.domain.dto.LightAttachmentDto;
import org.highmed.numportal.attachment.service.AttachmentService;
import org.highmed.numportal.domain.dto.Language;
import org.highmed.numportal.domain.dto.ProjectDto;
import org.highmed.numportal.domain.dto.ProjectInfoDto;
import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.dto.TemplateInfoDto;
import org.highmed.numportal.domain.dto.UserDetailsDto;
import org.highmed.numportal.domain.dto.ZarsInfoDto;
import org.highmed.numportal.domain.model.ExportType;
import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.ProjectStatus;
import org.highmed.numportal.domain.model.ProjectTransition;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.ProjectRepository;
import org.highmed.numportal.domain.repository.ProjectTransitionRepository;
import org.highmed.numportal.domain.specification.ProjectSpecification;
import org.highmed.numportal.mapper.ProjectMapper;
import org.highmed.numportal.service.atna.AtnaService;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.ehrbase.ResponseFilter;
import org.highmed.numportal.service.email.ZarsService;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.ForbiddenException;
import org.highmed.numportal.service.exception.ResourceNotFound;
import org.highmed.numportal.service.exception.SystemException;
import org.highmed.numportal.service.executors.CohortQueryLister;
import org.highmed.numportal.service.metric.ProjectsMetrics;
import org.highmed.numportal.service.notification.NotificationService;
import org.highmed.numportal.service.notification.dto.Notification;
import org.highmed.numportal.service.notification.dto.ProjectApprovalRequestNotification;
import org.highmed.numportal.service.notification.dto.ProjectCloseNotification;
import org.highmed.numportal.service.notification.dto.ProjectStartNotification;
import org.highmed.numportal.service.notification.dto.ProjectStatusChangeNotification;
import org.highmed.numportal.service.notification.dto.ProjectStatusChangeRequestNotification;
import org.highmed.numportal.service.policy.Policy;
import org.highmed.numportal.service.policy.ProjectPolicyService;
import org.highmed.numportal.service.util.ExportUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.AN_ISSUE_HAS_OCCURRED_CANNOT_EXECUTE_AQL;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_PROJECT;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_OWNER;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_ARCHIVE_PROJECT;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_DELETE_ATTACHMENTS_INVALID_PROJECT_STATUS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_DELETE_PROJECT;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_DELETE_PROJECT_INVALID_STATUS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_UPDATE_PROJECT_INVALID_PROJECT_STATUS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.DATA_EXPLORER_AVAILABLE_FOR_PUBLISHED_PROJECTS_ONLY;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.ERROR_CREATING_THE_PROJECT_PDF;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.INVALID_PROJECT_STATUS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.INVALID_PROJECT_STATUS_PARAM;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.MORE_THAN_ONE_TRANSITION_FROM_PUBLISHED_TO_CLOSED_FOR_PROJECT;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.NO_PERMISSIONS_TO_DELETE_ATTACHMENTS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.NO_PERMISSIONS_TO_EDIT_THIS_PROJECT;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.PROJECT_COHORT_CANNOT_BE_NULL;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.PROJECT_TEMPLATES_CANNOT_BE_NULL;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.RESEARCHER_NOT_APPROVED;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.RESEARCHER_NOT_FOUND;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectService {

  private static final String AUTHOR_NAME = "author";
  private static final String ORGANIZATION_NAME = "organization";
  private static final String PROJECT_NAME = "name";
  private static final String PROJECT_STATUS = "status";
  private static final String PROJECT_CREATE_DATE = "createDate";
  private final List<String> availableSortFields = Arrays.asList(PROJECT_NAME, AUTHOR_NAME, ORGANIZATION_NAME, PROJECT_STATUS, PROJECT_CREATE_DATE);
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

  private final ExportUtil exportUtil;

  private final ResponseFilter responseFilter;

  private final ProjectDocCreator projectDocCreator;

  private final ProjectMapper projectMapper;

  private final AttachmentService attachmentService;

  private final ProjectsMetrics projectsMetrics;


  @Transactional
  public boolean deleteProject(Long projectId, String userId, List<String> roles) throws ForbiddenException {
    userDetailsService.checkIsUserApproved(userId);

    var project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFound(ProjectService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, projectId)));

    if (project.hasEmptyOrDifferentOwner(userId) && !roles.contains(Roles.SUPER_ADMIN)) {
      throw new ForbiddenException(ProjectService.class, CANNOT_DELETE_PROJECT, String.format(CANNOT_DELETE_PROJECT, projectId));
    }

    if (project.isDeletable()) {
      attachmentService.deleteAllProjectAttachments(projectId, userId);
      projectRepository.deleteById(projectId);
      projectsMetrics.decreaseTotalNumber();
      log.info("Project {} was deleted by {}", projectId, userId);
    } else {
      throw new ForbiddenException(ProjectService.class, CANNOT_DELETE_PROJECT_INVALID_STATUS,
          String.format(CANNOT_DELETE_PROJECT_INVALID_STATUS, projectId, project.getStatus()));
    }
    return true;
  }

  @Transactional
  public boolean archiveProject(Long projectId, String userId, List<String> roles) {
    var user = userDetailsService.checkIsUserApproved(userId);

    var project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFound(ProjectService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, projectId)));

    if (project.hasEmptyOrDifferentOwner(userId) && !roles.contains(Roles.SUPER_ADMIN)) {
      throw new ForbiddenException(ProjectService.class, CANNOT_ARCHIVE_PROJECT, String.format(CANNOT_ARCHIVE_PROJECT, projectId));
    }
    ProjectStatus before = project.getStatus();
    validateStatus(project.getStatus(), ProjectStatus.ARCHIVED, roles);
    persistTransition(project, project.getStatus(), ProjectStatus.ARCHIVED, user);

    project.setStatus(ProjectStatus.ARCHIVED);
    projectRepository.save(project);
    projectsMetrics.changeStatus(before, ProjectStatus.ARCHIVED);
    return true;
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
   * @return The list of max requested count of the latest projects
   */
  public List<ProjectInfoDto> getLatestProjectsInfo(int count, List<String> roles) {

    if (count < 1) {
      return List.of();
    }

    List<Project> projects = projectRepository.findByStatusInOrderByCreateDateDesc(Arrays.asList(ProjectStatus.APPROVED,
        ProjectStatus.PUBLISHED, ProjectStatus.CLOSED), PageRequest.of(0, count));
    return projects.stream()
                   .map(project -> toProjectInfo(project, roles))
                   .collect(Collectors.toList());
  }

  public String retrieveData(String query, Long projectId, String userId, Boolean defaultConfiguration) {
    userDetailsService.checkIsUserApproved(userId);
    Project project = validateAndRetrieveProject(projectId, userId);
    log.info("Retrieve research data for project: {} by: user {}", projectId, userId);
    List<QueryResponseData> responseData;
    if (BooleanUtils.isTrue(defaultConfiguration)) {
      responseData =
          exportUtil.executeDefaultConfiguration(projectId, project.getCohort(), project.getTemplates());
    } else {
      responseData = executeCustomConfiguration(query, projectId, userId);
    }

    try {
      return mapper.writeValueAsString(responseData);
    } catch (JsonProcessingException e) {
      throw new SystemException(ProjectService.class, AN_ISSUE_HAS_OCCURRED_CANNOT_EXECUTE_AQL);
    }
  }


  private List<QueryResponseData> executeCustomConfiguration(String query, Long projectId, String userId) {
    List<QueryResponseData> response = executeAql(query, projectId, userId);
    return responseFilter.filterResponse(response);
  }


  public List<QueryResponseData> executeAql(String query, Long projectId, String userId) {
    List<QueryResponseData> queryResponseData;
    Project project = null;
    try {
      userDetailsService.checkIsUserApproved(userId);

      project = validateAndRetrieveProject(projectId, userId);

      Set<String> ehrIds =
          cohortService.executeCohort(project.getCohort().getId(), project.isUsedOutsideEu());

      AqlQuery aql = AqlQueryParser.parse(query);

      List<Policy> policies =
          exportUtil.collectProjectPolicies(ehrIds, project.getTemplates(), project.isUsedOutsideEu());
      projectPolicyService.apply(aql, policies);

      queryResponseData = ehrBaseService.executeRawQuery(aql, projectId);

    } catch (Exception e) {
      atnaService.logDataExport(userId, projectId, project, false);
      throw e;
    }
    atnaService.logDataExport(userId, projectId, project, true);
    return queryResponseData;
  }

  public StreamingResponseBody getExportResponseBody(
      String query,
      Long projectId,
      String userId,
      ExportType format,
      Boolean defaultConfiguration) {

    userDetailsService.checkIsUserApproved(userId);
    Project project = validateAndRetrieveProject(projectId, userId);
    List<QueryResponseData> response;

    if (BooleanUtils.isTrue(defaultConfiguration)) {
      response =
          exportUtil.executeDefaultConfiguration(projectId, project.getCohort(), project.getTemplates());
    } else {
      response = executeCustomConfiguration(query, projectId, userId);
    }

    if (format == ExportType.json) {
      return exportUtil.exportJson(response);
    } else {
      return exportUtil.exportCsv(response, projectId);
    }
  }

  public Optional<Project> getProjectById(String loggedInUserId, Long projectId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
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
    projectsMetrics.increaseTotalNumber();
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
  public Project createMultipartProject(ProjectDto projectDto, String userId, List<String> roles, MultipartFile[] files) {
    Project savedProject = createProject(projectDto, userId, roles);
    if (nonNull(files)) {
      try {
        LightAttachmentDto lightDto = LightAttachmentDto.builder()
                                                        .files(files)
                                                        .description(projectDto.getFilesDescription())
                                                        .build();
        attachmentService.saveAttachments(savedProject.getId(), userId, lightDto, true);
      } catch (IOException e) {
        log.error("Exception in createMultipartProject saveAttachments" + e.getMessage());
        throw new RuntimeException(e);
      }
    }
    return savedProject;
  }

  @Transactional
  public Project updateProject(ProjectDto projectDto, Long id, String userId, List<String> roles) {
    var user = userDetailsService.checkIsUserApproved(userId);

    var projectToEdit =
        projectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFound(ProjectService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, id)));

    if (ProjectStatus.ARCHIVED.equals(projectToEdit.getStatus())
        || ProjectStatus.CLOSED.equals(projectToEdit.getStatus())) {
      throw new ForbiddenException(ProjectService.class, CANNOT_UPDATE_PROJECT_INVALID_PROJECT_STATUS,
          String.format(CANNOT_UPDATE_PROJECT_INVALID_PROJECT_STATUS, id, projectToEdit.getStatus()));
    }

    if (CollectionUtils.isNotEmpty(roles)
        && Roles.isProjectLead(roles)
        && projectToEdit.isCoordinator(userId)) {
      return updateProjectAllFields(projectDto, roles, user, projectToEdit);
    } else if (CollectionUtils.isNotEmpty(roles) && Roles.isProjectApprover(roles)) {
      Project savedProject = updateProjectStatus(projectDto, roles, user, projectToEdit);
      deleteAttachments(projectDto.getAttachmentsToBeDeleted(), roles, user, savedProject);
      if (ProjectStatus.REVIEWING.equals(projectToEdit.getStatus())) {
        attachmentService.updateStatusChangeCounter(projectToEdit.getId());
      }
      return savedProject;
    } else {
      throw new ForbiddenException(ProjectService.class, NO_PERMISSIONS_TO_EDIT_THIS_PROJECT);
    }
  }

  @Transactional
  public Project updateMultipartProject(ProjectDto projectDto, Long id, String userId, List<String> roles, MultipartFile[] files) {
    Project updatedProject = updateProject(projectDto, id, userId, roles);
    if (nonNull(files)) {
      try {
        LightAttachmentDto lightDto = LightAttachmentDto.builder()
                                                        .files(files)
                                                        .description(projectDto.getFilesDescription())
                                                        .build();
        attachmentService.saveAttachments(updatedProject.getId(), userId, lightDto, false);
      } catch (IOException e) {
        log.error("Exception in updateMultipartProject saveAttachments" + e.getMessage());
        throw new RuntimeException(e);
      }
    }
    return updatedProject;
  }

  private Project updateProjectStatus(ProjectDto projectDto, List<String> roles, UserDetails user, Project projectToEdit) {

    var oldProjectStatus = projectToEdit.getStatus();

    validateStatus(projectToEdit.getStatus(), projectDto.getStatus(), roles);
    persistTransition(projectToEdit, projectToEdit.getStatus(), projectDto.getStatus(), user);
    projectToEdit.setStatus(projectDto.getStatus());
    var savedProject = projectRepository.save(projectToEdit);
    projectsMetrics.changeStatus(oldProjectStatus, savedProject.getStatus());
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
      projectsMetrics.changeStatus(oldStatus, savedProject.getStatus());
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

    deleteAttachments(projectDto.getAttachmentsToBeDeleted(), roles, user, projectToEdit);

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
    projectsMetrics.changeStatus(oldStatus, savedProject.getStatus());
    if (ProjectStatus.REVIEWING.equals(savedProject.getStatus())) {
      attachmentService.updateStatusChangeCounter(projectToEdit.getId());
    }
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

  private void deleteAttachments(Set<Long> attachmentsToBeDeleted, List<String> roles, UserDetails user, Project projectToEdit) {
    if (CollectionUtils.isNotEmpty(attachmentsToBeDeleted)) {
      if (Roles.isProjectLead(roles) && projectToEdit.isCoordinator(user.getUserId())) {
        if (ProjectStatus.DRAFT.equals(projectToEdit.getStatus()) || ProjectStatus.CHANGE_REQUEST.equals(projectToEdit.getStatus())) {
          attachmentService.deleteAttachments(attachmentsToBeDeleted, projectToEdit.getId(), user.getUserId(), false);
          log.info("Project lead {} removed attachments {} from project {}", user.getUserId(), attachmentsToBeDeleted, projectToEdit.getId());
        } else {
          log.error("Not allowed to delete attachments for project {} because status is {} ", projectToEdit.getId(), projectToEdit.getStatus());
          throw new ForbiddenException(ProjectService.class, CANNOT_DELETE_ATTACHMENTS_INVALID_PROJECT_STATUS,
              String.format(CANNOT_DELETE_ATTACHMENTS_INVALID_PROJECT_STATUS, projectToEdit.getStatus()));
        }
      } else if (Roles.isProjectApprover(roles)) {
        if (ProjectStatus.REVIEWING.equals(projectToEdit.getStatus())) {
          attachmentService.deleteAttachments(attachmentsToBeDeleted, projectToEdit.getId(), user.getUserId(), true);
          log.info("Project approver {} removed attachments {} from project {}", user.getUserId(), attachmentsToBeDeleted, projectToEdit.getId());
        } else {
          log.error(
              "Not allowed to delete attachments for project {} as project approver {} because status is {} ", projectToEdit.getId(),
              user.getUserId(), projectToEdit.getStatus());
          throw new ForbiddenException(ProjectService.class, CANNOT_DELETE_ATTACHMENTS_INVALID_PROJECT_STATUS,
              String.format(CANNOT_DELETE_ATTACHMENTS_INVALID_PROJECT_STATUS, projectToEdit.getStatus()));
        }
      } else {
        log.error("User {} is not allowed to delete attachments from project {}", user.getUserId(), projectToEdit.getId());
        throw new ForbiddenException(ProjectService.class, NO_PERMISSIONS_TO_DELETE_ATTACHMENTS);
      }
    }
  }

  private List<Notification> collectNotifications(Project project, ProjectStatus newStatus,
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
                                                  .coordinatorEmail(coordinator.getEmail())
                                                  .build();
            notifications.add(notification);
          });
    }

    if (isTransitionMadeByApprover(oldStatus, newStatus)) {

      var approver = userService.getUserById(approverUserId, false);

      if (isTransitionToChangeRequest(oldStatus, newStatus)) {
        ProjectStatusChangeRequestNotification notification = new ProjectStatusChangeRequestNotification(coordinator.getEmail(),
            coordinator.getFirstName(),
            coordinator.getLastName(), approver.getFirstName(), approver.getLastName(),
            project.getName(), newStatus, oldStatus, project.getId(), approver.getEmail());
        notifications.add(notification);
      } else {
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
                                           .approverEmail(approver.getEmail())
                                           .build();
        notifications.add(notification);
      }
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

  private boolean isTransitionToChangeRequest(ProjectStatus oldStatus, ProjectStatus newStatus) {
    return ProjectStatus.CHANGE_REQUEST.equals(newStatus) && !newStatus.equals(oldStatus);
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

  public Page<Project> getProjects(String userId, List<String> roles, SearchCriteria searchCriteria, Pageable pageable) {

    UserDetails loggedInUser = userDetailsService.checkIsUserApproved(userId);

    Sort sortBy = validateAndGetSort(searchCriteria);
    List<Project> projects;
    Page<Project> projectPage;
    Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Set<String> usersUUID = null;
    if (searchCriteria.getFilter() != null && searchCriteria.getFilter().containsKey(SearchCriteria.FILTER_SEARCH_BY_KEY)) {
      String searchValue = (String) searchCriteria.getFilter().get(SearchCriteria.FILTER_SEARCH_BY_KEY);
      usersUUID = userService.findUsersUUID(searchValue);
    }
    boolean sortByAuthor = searchCriteria.isSortByAuthor();
    if (sortByAuthor) {
      long count = projectRepository.count();
      // load all projects because sort by author should be done in memory
      pageRequest = PageRequest.of(0, count != 0 ? (int) count : 1);
    }
    String sortByField = searchCriteria.isValid() && StringUtils.isNotEmpty(searchCriteria.getSortBy()) ? searchCriteria.getSortBy() : "modifiedDate";
    Language language = Objects.nonNull(searchCriteria.getLanguage()) ? searchCriteria.getLanguage() : Language.de;
    ProjectSpecification projectSpecification = ProjectSpecification.builder()
                                                                    .filter(searchCriteria.getFilter())
                                                                    .roles(roles)
                                                                    .loggedInUserId(userId)
                                                                    .loggedInUserOrganizationId(
                                                                        loggedInUser.getOrganization() != null ? loggedInUser.getOrganization()
                                                                                                                             .getId() : null)
                                                                    .ownersUUID(usersUUID)
                                                                    .sortOrder(Objects.requireNonNull(sortBy.getOrderFor(sortByField)).ignoreCase())
                                                                    .language(language)
                                                                    .build();
    projectPage = projectRepository.findProjects(projectSpecification, pageRequest);
    projects = new ArrayList<>(projectPage.getContent());
    if (sortByAuthor) {
      sortProjects(projects, sortBy);
      projects = projects.stream()
                         .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                         .limit(pageable.getPageSize())
                         .collect(Collectors.toList());
    }
    return new PageImpl<>(projects, pageable, projectPage.getTotalElements());
  }

  private Sort validateAndGetSort(SearchCriteria searchCriteria) {
    if (searchCriteria.isValid() && StringUtils.isNotEmpty(searchCriteria.getSortBy())) {
      if (!availableSortFields.contains(searchCriteria.getSortBy())) {
        throw new BadRequestException(ProjectService.class, String.format("Invalid %s sortBy field for projects", searchCriteria.getSortBy()));
      }
      return Sort.by(
          Sort.Direction.valueOf(searchCriteria.getSort().toUpperCase()),
          searchCriteria.getSortBy());
    }
    return Sort.by(Sort.Direction.DESC, "modifiedDate");
  }

  private void sortProjects(List<Project> projects, Sort sortBy) {
    if (sortBy != null) {
      Sort.Order authorOrder = sortBy.getOrderFor(AUTHOR_NAME);
      if (authorOrder != null) {
        Comparator<Project> byAuthorName = Comparator.comparing(project -> {
          User coordinator = userService.getOwner(project.getCoordinator().getUserId());
          return Objects.requireNonNull(coordinator).getFullName().toUpperCase();
        });
        Sort.Direction sortOrder = authorOrder.getDirection();
        if (sortOrder.isAscending()) {
          projects.sort(Comparator.nullsLast(byAuthorName));
        } else {
          projects.sort(Comparator.nullsLast(byAuthorName.reversed()));
        }
      }
    }
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
          throw new BadRequestException(ProjectService.class, RESEARCHER_NOT_FOUND);
        }

        if (researcher.get().isNotApproved()) {
          throw new BadRequestException(ProjectService.class, RESEARCHER_NOT_APPROVED);
        }

        newResearchersList.add(researcher.get());
      }
    }
    return newResearchersList;
  }

  private void validateStatus(
      ProjectStatus initialStatus, ProjectStatus nextStatus, List<String> roles) {

    if (nextStatus == null) {
      throw new BadRequestException(ProjectService.class, INVALID_PROJECT_STATUS);
    }

    if (initialStatus == null) {
      if (!isValidInitialStatus(nextStatus)) {
        throw new BadRequestException(ProjectService.class, INVALID_PROJECT_STATUS_PARAM, String.format(INVALID_PROJECT_STATUS_PARAM, nextStatus));
      }
    } else if (initialStatus.nextStatusesAndRoles().containsKey(nextStatus)) {
      List<String> allowedRoles = initialStatus.nextStatusesAndRoles().get(nextStatus);

      Set<String> intersectionSet =
          roles.stream().distinct().filter(allowedRoles::contains).collect(Collectors.toSet());

      if (intersectionSet.isEmpty()) {
        throw new ForbiddenException(ProjectService.class, PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED,
            String.format(PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED, initialStatus, nextStatus));
      }
    } else {
      throw new BadRequestException(ProjectService.class, PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED,
          String.format(PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED, initialStatus, nextStatus));
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
      throw new ForbiddenException(ProjectService.class, CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_OWNER);
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
      throw new SystemException(ProjectService.class, MORE_THAN_ONE_TRANSITION_FROM_PUBLISHED_TO_CLOSED_FOR_PROJECT,
          String.format(MORE_THAN_ONE_TRANSITION_FROM_PUBLISHED_TO_CLOSED_FOR_PROJECT, project.getId()));
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
            .orElseThrow(() -> new ResourceNotFound(ProjectService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, projectId)));

    if (project.getStatus() == null || !project.getStatus().equals(ProjectStatus.PUBLISHED)) {
      throw new ForbiddenException(ProjectService.class, DATA_EXPLORER_AVAILABLE_FOR_PUBLISHED_PROJECTS_ONLY);
    }

    if (!project.isProjectResearcher(userId)) {
      throw new ForbiddenException(ProjectService.class, CANNOT_ACCESS_THIS_PROJECT);
    }

    if (project.getCohort() == null) {
      throw new BadRequestException(ProjectService.class, PROJECT_COHORT_CANNOT_BE_NULL,
          String.format(PROJECT_COHORT_CANNOT_BE_NULL, project.getId()));
    }

    if (project.getTemplates() == null) {
      throw new BadRequestException(ProjectService.class, PROJECT_TEMPLATES_CANNOT_BE_NULL,
          String.format(PROJECT_TEMPLATES_CANNOT_BE_NULL, project.getId()));
    }
    return project;
  }

  public byte[] getInfoDocBytes(Long id, String userId, Locale locale) {
    userDetailsService.checkIsUserApproved(userId);
    Project project =
        projectRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestException(ProjectService.class, PROJECT_NOT_FOUND));
    ProjectDto projectDto = projectMapper.convertToDto(project);
    try {
      return projectDocCreator.getDocBytesOfProject(projectDto, locale);
    } catch (IOException e) {
      throw new SystemException(ProjectService.class, ERROR_CREATING_THE_PROJECT_PDF,
          String.format(ERROR_CREATING_THE_PROJECT_PDF, e.getMessage()));
    }
  }
}
