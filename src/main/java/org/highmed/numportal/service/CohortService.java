package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.CohortDto;
import org.highmed.numportal.domain.dto.CohortGroupDto;
import org.highmed.numportal.domain.dto.CohortSizeDto;
import org.highmed.numportal.domain.dto.TemplateSizeRequestDto;
import org.highmed.numportal.domain.model.Cohort;
import org.highmed.numportal.domain.model.CohortGroup;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.ProjectStatus;
import org.highmed.numportal.domain.repository.CohortRepository;
import org.highmed.numportal.domain.repository.ProjectRepository;
import org.highmed.numportal.properties.PrivacyProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.ComparisonOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.WhereCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.Operand;
import org.ehrbase.openehr.sdk.aql.dto.operand.QueryParameter;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.highmed.numportal.service.ehrbase.EhrBaseService;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.ForbiddenException;
import org.highmed.numportal.service.exception.PrivacyException;
import org.highmed.numportal.service.exception.ResourceNotFound;
import org.highmed.numportal.service.executors.CohortExecutor;
import org.highmed.numportal.service.policy.EhrPolicy;
import org.highmed.numportal.service.policy.Policy;
import org.highmed.numportal.service.policy.ProjectPolicyService;
import org.highmed.numportal.service.policy.TemplatesPolicy;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.*;

@Slf4j
@Service
@AllArgsConstructor
public class CohortService {

  private final CohortRepository cohortRepository;
  private final CohortExecutor cohortExecutor;
  private final UserDetailsService userDetailsService;
  private final ModelMapper modelMapper;
  private final AqlService aqlService;
  private final ProjectRepository projectRepository;
  private final PrivacyProperties privacyProperties;
  private final ProjectPolicyService policyService;
  private final EhrBaseService ehrBaseService;
  private final ContentService contentService;
  private final TemplateService templateService;

  public static final String GET_PATIENTS_PER_CLINIC =
      "SELECT e/ehr_id/value as patient_id "
          + "FROM EHR e CONTAINS COMPOSITION c "
          + "WHERE c/context/health_care_facility/name = '%s'"
          + "AND e/ehr_id/value MATCHES {%s} ";
  public static final String GET_PATIENTS_PER_AGE_INTERVAL =
      "SELECT count(e/ehr_id/value) "
          + "FROM EHR e contains OBSERVATION o0[openEHR-EHR-OBSERVATION.age.v0] "
          + "WHERE o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value >= 'P%dY' "
          + "AND o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value < 'P%dY'"
          + "AND e/ehr_id/value MATCHES {%s} ";
  private static final String AGE_INTERVAL_LABEL = "%d-%d";
  private static final int MAX_AGE = 122;
  private static final int AGE_INTERVAL = 10;

  public Cohort getCohort(Long cohortId, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    return cohortRepository.findById(cohortId).orElseThrow(
            () -> new ResourceNotFound(CohortService.class, COHORT_NOT_FOUND, String.format(COHORT_NOT_FOUND, cohortId)));
  }

  public Cohort createCohort(CohortDto cohortDto, String userId) {
    userDetailsService.checkIsUserApproved(userId);

    Project project =
        projectRepository
            .findById(cohortDto.getProjectId())
            .orElseThrow(
                () -> new ResourceNotFound(ProjectService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, cohortDto.getProjectId())));

    checkProjectModifiable(project, userId);

    Cohort cohort =
        Cohort.builder()
            .name(cohortDto.getName())
            .description(cohortDto.getDescription())
            .project(project)
            .cohortGroup(convertToCohortGroupEntity(cohortDto.getCohortGroup()))
            .build();

    project.setCohort(cohort);
    log.info("Cohort created by user {}", userId);
    return cohortRepository.save(cohort);
  }

  public Cohort toCohort(CohortDto cohortDto) {
    return Cohort.builder()
        .name(cohortDto.getName())
        .description(cohortDto.getDescription())
        .cohortGroup(convertToCohortGroupEntity(cohortDto.getCohortGroup()))
        .build();
  }

  public Set<String> executeCohort(long cohortId, Boolean allowUsageOutsideEu) {
    Optional<Cohort> cohort = cohortRepository.findById(cohortId);
    return cohortExecutor.execute(
        cohort.orElseThrow(() -> new BadRequestException(CohortService.class, COHORT_NOT_FOUND, String.format(COHORT_NOT_FOUND, cohortId))),
        allowUsageOutsideEu);
  }

  public Set<String> executeCohort(Cohort cohort, Boolean allowUsageOutsideEu) {
    return cohortExecutor.execute(cohort, allowUsageOutsideEu);
  }

  public long getCohortGroupSize(CohortGroupDto cohortGroupDto, String userId, Boolean allowUsageOutsideEu) {
    userDetailsService.checkIsUserApproved(userId);
    Set<String> ehrIds = getCohortGroupEhrIds(cohortGroupDto, allowUsageOutsideEu);
    return ehrIds.size();
  }

  public int getRoundedSize(long size) {
    return Math.round((float) size / 10) * 10;
  }

  public Map<String, Integer> getSizePerTemplates(
      String userId, TemplateSizeRequestDto requestDto) {
    userDetailsService.checkIsUserApproved(userId);

    Cohort cohort =
        Cohort.builder()
            .cohortGroup(convertToCohortGroupEntity(requestDto.getCohortDto().getCohortGroup()))
            .build();

    Set<String> ehrIds = cohortExecutor.execute(cohort, false);
    if (ehrIds.size() < privacyProperties.getMinHits()) {
      log.warn(RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
      throw new PrivacyException(CohortService.class, RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
    }

    return determineTemplatesHits(ehrIds, requestDto.getTemplateIds());
  }

  private Map<String, Integer> determineTemplatesHits(
      Set<String> ehrIds, List<String> templateIds) {
    Map<String, Integer> hits = new HashMap<>();
    templateIds.forEach(templateId -> getTemplateHits(ehrIds, hits, templateId));
    return hits;
  }

  private void getTemplateHits(Set<String> ehrIds, Map<String, Integer> hits, String templateId) {
    try {
      AqlQuery aql = templateService.createSelectCompositionQuery(templateId);

      List<Policy> policies = new LinkedList<>();
      policies.add(EhrPolicy.builder().cohortEhrIds(ehrIds).build());
      policies.add(TemplatesPolicy.builder().templatesMap(Map.of(templateId, templateId)).build());
      policyService.apply(aql, policies);

      Set<String> templateHits =
          ehrBaseService.retrieveEligiblePatientIds(AqlRenderer.render(aql));
      hits.put(templateId, templateHits != null ? templateHits.size() : 0);

    } catch (Exception e) {
      log.error(e.getMessage(), e);

      if (StringUtils.isNotEmpty(templateId)) {
        hits.put(templateId, -1);
      }
    }
  }

  public Cohort updateCohort(CohortDto cohortDto, Long cohortId, String userId) {
    userDetailsService.checkIsUserApproved(userId);

    Cohort cohortToEdit =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(() -> new ResourceNotFound(CohortService.class, COHORT_NOT_FOUND, String.format(COHORT_NOT_FOUND, cohortId)));

    Project project = cohortToEdit.getProject();

    checkProjectModifiable(project, userId);

    cohortToEdit.setCohortGroup(convertToCohortGroupEntity(cohortDto.getCohortGroup()));
    cohortToEdit.setDescription(cohortDto.getDescription());
    cohortToEdit.setName(cohortDto.getName());
    log.info("User {} updated cohort {}", userId, cohortId);
    return cohortRepository.save(cohortToEdit);
  }

  private void checkProjectModifiable(Project project, String userId) {
    if (project.hasEmptyOrDifferentOwner(userId)) {
      throw new ForbiddenException(AqlService.class, CHANGING_COHORT_ONLY_ALLOWED_BY_THE_OWNER_OF_THE_PROJECT);
    }

    if (project.getStatus() != ProjectStatus.DRAFT
        && project.getStatus() != ProjectStatus.PENDING
        && project.getStatus() != ProjectStatus.CHANGE_REQUEST) {
      throw new ForbiddenException(AqlService.class, COHORT_CHANGE_ONLY_ALLOWED_ON_PROJECT_STATUS_DRAFT_OR_PENDING);
    }
  }

  private void validateCohortParameters(CohortGroupDto cohortGroupDto) {
    if (cohortGroupDto.isGroup() && CollectionUtils.isEmpty(cohortGroupDto.getChildren())) {
      throw new BadRequestException(CohortService.class, INVALID_COHORT_GROUP_CHILDREN_MISSING);
    }
    if (cohortGroupDto.isAql()) {
      if (Objects.isNull(cohortGroupDto.getQuery())) {
        throw new BadRequestException(CohortGroup.class, INVALID_COHORT_GROUP_AQL_MISSING);
      }
      Set<String> parameterNames = new HashSet<>();
      AqlQuery aqlDto = AqlQueryParser.parse(cohortGroupDto.getQuery().getQuery());
      WhereCondition conditionDto = aqlDto.getWhere();
      if (conditionDto instanceof ComparisonOperatorCondition) {
        Operand value = ((ComparisonOperatorCondition) conditionDto).getValue();
        if (value instanceof QueryParameter parameterValue) {
          parameterNames.add(parameterValue.getName());
        }
      } else if (conditionDto instanceof LogicalOperatorCondition) {
        List<WhereCondition> values = ((LogicalOperatorCondition) conditionDto).getValues();
        for (WhereCondition v : values) {
          if (v instanceof ComparisonOperatorCondition) {
            Operand value = ((ComparisonOperatorCondition) v).getValue();
            if (value instanceof QueryParameter parameterValue) {
              parameterNames.add(parameterValue.getName());
            }
          }
        }
      }
      if (CollectionUtils.isNotEmpty(parameterNames) && MapUtils.isEmpty(cohortGroupDto.getParameters())) {
        log.error("The query is invalid. The value of parameter(s) {} is missing", parameterNames);
        throw new BadRequestException(CohortService.class, INVALID_COHORT_GROUP_AQL_MISSING_PARAMETERS);
      } else if (CollectionUtils.isNotEmpty(parameterNames) && MapUtils.isNotEmpty(cohortGroupDto.getParameters())) {
        Set<String> receivedParams = cohortGroupDto.getParameters().keySet();
        if (!receivedParams.containsAll(parameterNames)) {
          parameterNames.removeAll(receivedParams);
          log.error("The query is invalid. The value of parameter {} is missing", parameterNames);
          throw new BadRequestException(CohortService.class, INVALID_COHORT_GROUP_AQL_MISSING_PARAMETERS);
        }
      }
    }
    if (CollectionUtils.isNotEmpty(cohortGroupDto.getChildren())) {
      cohortGroupDto.getChildren()
              .forEach(this::validateCohortParameters);
    }
  }

  private CohortGroup convertToCohortGroupEntity(CohortGroupDto cohortGroupDto) {
    if (cohortGroupDto == null) {
      throw new BadRequestException(CohortGroup.class, COHORT_GROUP_CANNOT_BE_EMPTY);
    }

    CohortGroup cohortGroup = modelMapper.map(cohortGroupDto, CohortGroup.class);
    cohortGroup.setId(null);

    if (cohortGroupDto.isAql()) {
      if (cohortGroupDto.getQuery() != null && cohortGroupDto.getQuery().getId() != null) {
        if (!aqlService.existsById(cohortGroupDto.getQuery().getId())) {
          throw new BadRequestException(CohortGroup.class, INVALID_AQL_ID,
                  String.format("%s: %s", INVALID_AQL_ID, cohortGroupDto.getQuery().getId()));
        }
      } else {
        throw new BadRequestException(CohortGroup.class, INVALID_COHORT_GROUP_AQL_MISSING);
      }
    }

    if (cohortGroupDto.isGroup()) {
      if (CollectionUtils.isNotEmpty(cohortGroup.getChildren())) {
        cohortGroup.setChildren(
                cohortGroupDto.getChildren().stream()
                        .map(
                                child -> {
                                  CohortGroup cohortGroupChild = convertToCohortGroupEntity(child);
                                  cohortGroupChild.setParent(cohortGroup);
                                  return cohortGroupChild;
                                })
                        .collect(Collectors.toList()));
      } else {
        throw new BadRequestException(CohortService.class, INVALID_COHORT_GROUP_CHILDREN_MISSING);
      }
    }
    return cohortGroup;
  }

  public CohortSizeDto getCohortGroupSizeWithDistribution(CohortGroupDto cohortGroupDto, String userId, Boolean allowUsageOutsideEu) {
    userDetailsService.checkIsUserApproved(userId);
    Set<String> ehrIds = getCohortGroupEhrIds(cohortGroupDto, allowUsageOutsideEu);
    int count = ehrIds.size();
    if (count == 0) {
      return CohortSizeDto.builder().build();
    }

    String idsString = "'" + String.join("','", ehrIds) + "'";

    var hospitals = getSizesPerHospital(userId, idsString);

    var ageGroups = getSizesPerAgeGroup(idsString);

    return CohortSizeDto.builder().hospitals(hospitals).ages(ageGroups).count(count).build();
  }

  private Set<String> getCohortGroupEhrIds(CohortGroupDto cohortGroupDto, Boolean allowUsageOutsideEu) {
    CohortGroup cohortGroup = convertToCohortGroupEntity(cohortGroupDto);
    validateCohortParameters(cohortGroupDto);
    Set<String> ehrIds = cohortExecutor.executeGroup(cohortGroup, allowUsageOutsideEu);
    if (ehrIds.size() < privacyProperties.getMinHits()) {
      log.warn(RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
      throw new PrivacyException(CohortService.class, RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
    }
    return ehrIds;
  }

  private Map<String, Integer> getSizesPerAgeGroup(String idsString) {
    Map<String, Integer> sizes = new LinkedHashMap<>();
    for (int age = 0; age < MAX_AGE; age += AGE_INTERVAL) {
      QueryResponseData queryResponseData =
          ehrBaseService.executePlainQuery(
              String.format(GET_PATIENTS_PER_AGE_INTERVAL, age, age + AGE_INTERVAL, idsString));
      List<List<Object>> rows = queryResponseData.getRows();
      String range = String.format(AGE_INTERVAL_LABEL, age, age + AGE_INTERVAL);
      if (rows == null || rows.get(0) == null || rows.get(0).get(0) == null) {
        sizes.put(range, 0);
      } else {
        sizes.put(range, (Integer) rows.get(0).get(0));
      }
    }
    return sizes;
  }

  private Map<String, Integer> getSizesPerHospital(String loggedInUserId, String idsString) {

    Map<String, Integer> sizes = new LinkedHashMap<>();
    List<String> clinics = contentService.getClinics(loggedInUserId);
    if (CollectionUtils.isNotEmpty(clinics)) {
      for (String clinic : clinics) {
        if (Objects.nonNull(clinic)) {
          QueryResponseData queryResponseData =
                  ehrBaseService.executePlainQuery(String.format(GET_PATIENTS_PER_CLINIC, clinic, idsString));
          List<List<Object>> rows = queryResponseData.getRows();
          if (rows == null) {
            sizes.put(clinic, 0);
          } else {
            sizes.put(clinic, rows.size());
          }
        }
      }
    }
    return sizes;
  }
}
