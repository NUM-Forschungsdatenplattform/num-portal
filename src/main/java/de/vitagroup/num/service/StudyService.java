package de.vitagroup.num.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.aql.parser.AqlToDtoParser;
import org.ehrbase.response.openehr.QueryResponseData;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StudyService {

  private final StudyRepository studyRepository;
  private final UserDetailsService userDetailsService;
  private final EhrBaseService ehrBaseService;
  private final ObjectMapper mapper;
  private final CohortService cohortService;
  private static final String EHR_ID_PATH = "/ehr_id/value";
  private static final String TEMPLATE_ID_PATH = "/archetype_details/template_id/value";
  private static final String COMPOSITION_IDENTIFIER = "c";
  private static final String COMPOSITION_ARCHETYPE_ID = "COMPOSITION";

  public String executeAqlAndJsonify(String query, Long studyId, String userId) {
    QueryResponseData response = executeAql(query, studyId, userId);
    try {
      return mapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new SystemException("An issue has occurred, cannot execute aql.");
    }
  }

  public QueryResponseData executeAql(String query, Long studyId, String userId) {
    userDetailsService.validateAndReturnUserDetails(userId);

    Study study =
        studyRepository
            .findById(studyId)
            .orElseThrow(() -> new ResourceNotFound("Study not found: " + studyId));

    if (!study.isStudyResearcher(userId)) {
      throw new ForbiddenException("Cannot access this study");
    }

    String restrictedQuery = restrictQueryToStudy(query, study);

    return ehrBaseService.executeRawQuery(restrictedQuery);
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

  public Optional<Study> getStudyById(Long studyId) {
    return studyRepository.findById(studyId);
  }

  public boolean exists(Long studyId) {
    return studyRepository.existsById(studyId);
  }

  public Study createStudy(StudyDto studyDto, String userId, List<String> roles) {

    UserDetails coordinator = userDetailsService.validateAndReturnUserDetails(userId);

    Study study = Study.builder().build();

    setTemplates(study, studyDto);
    setResearchers(study, studyDto);

    validateStatus(null, studyDto.getStatus(), roles);
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

    userDetailsService.validateAndReturnUserDetails(userId);

    Study studyToEdit =
        studyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFound("Study not found: " + id));

    setTemplates(studyToEdit, studyDto);
    setResearchers(studyToEdit, studyDto);

    validateStatus(studyToEdit.getStatus(), studyDto.getStatus(), roles);
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

  public String getCsvFilename(Long studyId) {
    return String.format(
        "Study_%d_%s.csv",
        studyId,
        LocalDateTime.now()
            .truncatedTo(ChronoUnit.MINUTES)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }

  private String restrictQueryToStudy(String query, Study study) {
    return restrictToStudyTemplates(restrictToCohortEhrIds(query, study), study.getTemplates());
  }

  public String restrictToStudyTemplates(String query, Map<String, String> templatesMap) {

    if (MapUtils.isEmpty(templatesMap)) {
      throw new BadRequestException("No templates attached to this study");
    }

    ArrayList<String> templates = new ArrayList<>(templatesMap.keySet());

    AqlDto aqlDto = new AqlToDtoParser().parse(query);

    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(TEMPLATE_ID_PATH);

    Integer compositionIdentifier = findComposition((ContainmentDto) aqlDto.getContains());

    if (compositionIdentifier != null) {
      select.setContainmentId(compositionIdentifier);
    } else {
      Integer nextContainmentId = findNextContainmentId((ContainmentDto) aqlDto.getContains());
      select.setContainmentId(nextContainmentId);

      ContainmentDto contains = new ContainmentDto();
      contains.setId(nextContainmentId);
      contains.setArchetypeId(COMPOSITION_ARCHETYPE_ID);

      aqlDto.setContains(contains);
    }

    MatchesOperatorDto matches = new MatchesOperatorDto();
    matches.setStatement(select);
    matches.setValues(toSimpleValueList(templates));

    ConditionLogicalOperatorDto newWhere = new ConditionLogicalOperatorDto();
    newWhere.setValues(new ArrayList<>());

    if (aqlDto.getWhere() != null) {
      newWhere.setSymbol(ConditionLogicalOperatorSymbol.AND);
      newWhere.getValues().add(aqlDto.getWhere());
    }

    newWhere.getValues().add(matches);
    aqlDto.setWhere(newWhere);

    return new AqlBinder().bind(aqlDto).getLeft().buildAql();
  }

  public String restrictToCohortEhrIds(String query, Study study) {
    if (study.getCohort() == null) {
      throw new BadRequestException("Study cohort cannot be empty");
    }

    Set<String> ehrIds = cohortService.executeCohort(study.getCohort().getId());

    if (CollectionUtils.isEmpty(ehrIds)) {
      throw new BadRequestException("Cohort size cannot be 0");
    }

    AqlDto aqlDto = new AqlToDtoParser().parse(query);

    SelectFieldDto select = new SelectFieldDto();
    select.setAqlPath(EHR_ID_PATH);
    select.setContainmentId(aqlDto.getEhr().getContainmentId());

    MatchesOperatorDto matches = new MatchesOperatorDto();
    matches.setStatement(select);
    matches.setValues(toSimpleValueList(ehrIds));

    ConditionLogicalOperatorDto newWhere = new ConditionLogicalOperatorDto();
    newWhere.setValues(new ArrayList<>());
    ConditionDto where = aqlDto.getWhere();

    if (where != null) {
      newWhere.setSymbol(ConditionLogicalOperatorSymbol.AND);
      newWhere.getValues().add(where);
    }

    newWhere.getValues().add(matches);
    aqlDto.setWhere(newWhere);
    return new AqlBinder().bind(aqlDto).getLeft().buildAql();
  }

  public Integer findComposition(ContainmentDto dto) {
    if (dto == null) {
      return null;
    }
    if (dto.getArchetypeId().contains(COMPOSITION_ARCHETYPE_ID)) {
      return dto.getId();
    } else {
      return findComposition((ContainmentDto) dto.getContains());
    }
  }

  public Integer findNextContainmentId(ContainmentDto dto) {
    if (dto == null) {
      return 1;
    }
    if (dto.getContains() != null) {
      return findNextContainmentId((ContainmentDto) dto.getContains());
    } else {
      return dto.getId() + 1;
    }
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
}
