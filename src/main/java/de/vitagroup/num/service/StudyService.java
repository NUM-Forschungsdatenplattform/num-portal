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
import lombok.AllArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
@AllArgsConstructor
public class StudyService {

  private final StudyRepository studyRepository;

  private final UserDetailsService userDetailsService;

  private final EhrBaseService ehrBaseService;

  private final ObjectMapper mapper;

  private final CohortService cohortService;

  private final AtnaService atnaService;

  private final UserService userService;

  private static final String EHR_ID_PATH = "/ehr_id/value";
  private static final String TEMPLATE_ID_PATH = "/archetype_details/template_id/value";
  private static final String COMPOSITION_ARCHETYPE_ID = "COMPOSITION";
  private static final String CSV_FILE_ENDING = ".csv";
  private static final String JSON_FILE_ENDING = ".json";
  private static final String CSV_MEDIA_TYPE = "text/csv";

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
    QueryResponseData response = executeAql(query, studyId, userId);
    try {
      return mapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new SystemException("An issue has occurred, cannot execute aql.");
    }
  }

  public QueryResponseData executeAql(String query, Long studyId, String userId) {
    QueryResponseData queryResponseData;
    Study study = null;
    try {
      userDetailsService.validateAndReturnUserDetails(userId);

      study =
          studyRepository
              .findById(studyId)
              .orElseThrow(() -> new ResourceNotFound("Study not found: " + studyId));

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

  public void streamResponseAsCsv(QueryResponseData queryResponseData, OutputStream outputStream) {
    List<String> paths = new ArrayList<>();

    for (Map<String, String> column : queryResponseData.getColumns()) {
      paths.add(column.get("path"));
    }
    try (CSVPrinter printer =
        CSVFormat.EXCEL
            .withHeader(paths.toArray(new String[] {}))
            .print(new OutputStreamWriter(outputStream))) {

      for (List<Object> row : queryResponseData.getRows()) {
        printer.printRecord(row);
      }
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
    QueryResponseData queryResponseData = executeAql(query, studyId, userId);
    return outputStream -> streamResponseAsCsv(queryResponseData, outputStream);
  }

  public MultiValueMap<String, String> getExportHeaders(ExportType format, Long studyId) {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    String fileEnding;
    if (format == ExportType.json) {
      fileEnding = JSON_FILE_ENDING;
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    } else {
      fileEnding = CSV_FILE_ENDING;
      headers.add(HttpHeaders.CONTENT_TYPE, CSV_MEDIA_TYPE);
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

  public Study createStudy(StudyDto studyDto, String userId, List<String> roles) {

    UserDetails coordinator = userDetailsService.validateAndReturnUserDetails(userId);

    Study study = Study.builder().build();

    validateStatus(null, studyDto.getStatus(), roles);
    persistTransition(study, study.getStatus(), studyDto.getStatus(), coordinator);

    setTemplates(study, studyDto);
    setResearchers(study, studyDto);

    study.setStatus(studyDto.getStatus());
    study.setName(studyDto.getName());
    study.setDescription(studyDto.getDescription());
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

    return studyRepository.save(study);
  }

  public Study updateStudy(StudyDto studyDto, Long id, String userId, List<String> roles) {

    UserDetails user = userDetailsService.validateAndReturnUserDetails(userId);

    Study studyToEdit =
        studyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFound("Study not found: " + id));

    validateStatus(studyToEdit.getStatus(), studyDto.getStatus(), roles);
    persistTransition(studyToEdit, studyToEdit.getStatus(), studyDto.getStatus(), user);

    setTemplates(studyToEdit, studyDto);
    setResearchers(studyToEdit, studyDto);

    studyToEdit.setStatus(studyDto.getStatus());
    studyToEdit.setName(studyDto.getName());
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

    return studyRepository.save(studyToEdit);
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
            "Study_%d_%s",
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

    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(TEMPLATE_ID_PATH);

    ContainmentExpresionDto contains = aql.getContains();
    Integer nextContainmentId = findNextContainmentId(contains);
    if (contains != null) {
      Integer compositionIdentifier = findComposition(contains);
      if (compositionIdentifier != null) {
        select.setContainmentId(compositionIdentifier);
      } else {

        select.setContainmentId(nextContainmentId);

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
    } else {
      ContainmentDto composition = new ContainmentDto();
      composition.setId(nextContainmentId);
      composition.setArchetypeId(COMPOSITION_ARCHETYPE_ID);
      aql.setContains(composition);
      select.setContainmentId(nextContainmentId);
    }

    List<Value> templateValues = toSimpleValueList(templatesMap.keySet());
    extendWhereClause(aql, select, templateValues);

    return new AqlBinder().bind(aql).getLeft().buildAql();
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

    extendWhereClause(aql, select, toSimpleValueList(ehrIds));

    return new AqlBinder().bind(aql).getLeft().buildAql();
  }

  private void extendWhereClause(AqlDto aql, SelectFieldDto select, List<Value> values) {
    MatchesOperatorDto matches = new MatchesOperatorDto();
    matches.setStatement(select);
    matches.setValues(values);

    ConditionLogicalOperatorDto newWhere = new ConditionLogicalOperatorDto();
    newWhere.setValues(new ArrayList<>());
    ConditionDto where = aql.getWhere();

    if (where != null) {
      newWhere.setSymbol(ConditionLogicalOperatorSymbol.AND);
      newWhere.getValues().add(where);
    }

    newWhere.getValues().add(matches);
    aql.setWhere(newWhere);
  }

  private Integer findComposition(ContainmentExpresionDto dto) {
    if (dto == null) {
      return null;
    }

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
          return containmentDto.getId();
        }

        if (containmentDto.getContains() != null) {
          queue.add(containmentDto.getContains());
        }
      }
    }
    return null;
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

  private void setResearchers(Study study, StudyDto studyDto) {
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
    study.setResearchers(newResearchersList);
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
}
