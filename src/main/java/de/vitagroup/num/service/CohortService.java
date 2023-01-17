package de.vitagroup.num.service;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.dto.CohortSizeDto;
import de.vitagroup.num.domain.dto.TemplateSizeRequestDto;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.executors.CohortExecutor;
import de.vitagroup.num.service.policy.EhrPolicy;
import de.vitagroup.num.service.policy.Policy;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import de.vitagroup.num.service.policy.TemplatesPolicy;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.PrivacyException;
import de.vitagroup.num.service.exception.ResourceNotFound;

import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.response.openehr.QueryResponseData;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CHANGING_COHORT_ONLY_ALLOWED_BY_THE_OWNER_OF_THE_PROJECT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COHORT_CHANGE_ONLY_ALLOWED_ON_PROJECT_STATUS_DRAFT_OR_PENDING;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COHORT_GROUP_CANNOT_BE_EMPTY;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COHORT_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL_ID;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_COHORT_GROUP_AQL_MISSING;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.RESULTS_WITHHELD_FOR_PRIVACY_REASONS;

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

  private static final String GET_PATIENTS_PER_CLINIC =
      "SELECT e/ehr_id/value as patient_id "
          + "FROM EHR e CONTAINS COMPOSITION c "
          + "WHERE c/context/health_care_facility/name = '%s'"
          + "AND e/ehr_id/value MATCHES {%s} ";
  private static final String GET_PATIENTS_PER_AGE_INTERVAL =
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

  public long getCohortGroupSize(
      CohortGroupDto cohortGroupDto, String userId, Boolean allowUsageOutsideEu) {
    userDetailsService.checkIsUserApproved(userId);

    CohortGroup cohortGroup = convertToCohortGroupEntity(cohortGroupDto);
    Set<String> ehrIds = cohortExecutor.executeGroup(cohortGroup, allowUsageOutsideEu);
    if (ehrIds.size() < privacyProperties.getMinHits()) {
      throw new PrivacyException(CohortService.class, RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
    }
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
      AqlDto aql = templateService.createSelectCompositionQuery(templateId);

      List<Policy> policies = new LinkedList<>();
      policies.add(EhrPolicy.builder().cohortEhrIds(ehrIds).build());
      policies.add(TemplatesPolicy.builder().templatesMap(Map.of(templateId, templateId)).build());
      policyService.apply(aql, policies);

      Set<String> templateHits =
          ehrBaseService.retrieveEligiblePatientIds(new AqlBinder().bind(aql).getLeft().buildAql());
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

  private CohortGroup convertToCohortGroupEntity(CohortGroupDto cohortGroupDto) {
    if (cohortGroupDto == null) {
      throw new BadRequestException(CohortGroup.class, COHORT_GROUP_CANNOT_BE_EMPTY);
    }

    CohortGroup cohortGroup = modelMapper.map(cohortGroupDto, CohortGroup.class);
    cohortGroup.setId(null);

    if (cohortGroupDto.isAql()) {
      if (cohortGroupDto.getQuery() != null && cohortGroupDto.getQuery().getId() != null) {

        if (!aqlService.existsById(cohortGroupDto.getQuery().getId())) {
          throw new BadRequestException(
                CohortGroup.class, INVALID_AQL_ID, String.format("%s: %s", INVALID_AQL_ID, cohortGroupDto.getQuery().getId()));
        }

      } else {
        throw new BadRequestException(CohortGroup.class, INVALID_COHORT_GROUP_AQL_MISSING);
      }
    }

    if (cohortGroupDto.isGroup()) {
      cohortGroup.setChildren(
          cohortGroupDto.getChildren().stream()
              .map(
                  child -> {
                    CohortGroup cohortGroupChild = convertToCohortGroupEntity(child);
                    cohortGroupChild.setParent(cohortGroup);
                    return cohortGroupChild;
                  })
              .collect(Collectors.toList()));
    }

    return cohortGroup;
  }

  public CohortSizeDto getCohortGroupSizeWithDistribution(
      CohortGroupDto cohortGroupDto, String userId, Boolean allowUsageOutsideEu) {
    userDetailsService.checkIsUserApproved(userId);

    CohortGroup cohortGroup = convertToCohortGroupEntity(cohortGroupDto);
    Set<String> ehrIds = cohortExecutor.executeGroup(cohortGroup, allowUsageOutsideEu);
    if (ehrIds.size() < privacyProperties.getMinHits()) {
      throw new PrivacyException(CohortService.class, RESULTS_WITHHELD_FOR_PRIVACY_REASONS);
    }
    int count = ehrIds.size();

    String idsString = "'" + String.join("','", ehrIds) + "'";

    var hospitals = getSizesPerHospital(idsString);

    var ageGroups = getSizesPerAgeGroup(idsString);

    return CohortSizeDto.builder().hospitals(hospitals).ages(ageGroups).count(count).build();
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

  private Map<String, Integer> getSizesPerHospital(String idsString) {

    Map<String, Integer> sizes = new LinkedHashMap<>();
    List<String> clinics = contentService.getClinics();
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
