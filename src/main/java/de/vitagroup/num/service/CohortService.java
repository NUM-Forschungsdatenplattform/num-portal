package de.vitagroup.num.service;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
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
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.PrivacyException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.EhrDto;
import org.ehrbase.aql.dto.select.SelectDto;
import org.ehrbase.aql.dto.select.SelectFieldDto;
import org.ehrbase.aqleditor.dto.containment.ContainmentDto;
import org.ehrbase.aqleditor.service.AqlEditorContainmentService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

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
  private final AqlEditorContainmentService aqlEditorContainmentService;
  private final ProjectPolicyService policyService;
  private final EhrBaseService ehrBaseService;

  public Cohort getCohort(Long cohortId, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    return cohortRepository.findById(cohortId).orElseThrow(ResourceNotFound::new);
  }

  public Cohort createCohort(CohortDto cohortDto, String userId) {
    userDetailsService.checkIsUserApproved(userId);

    Project project =
        projectRepository
            .findById(cohortDto.getProjectId())
            .orElseThrow(
                () -> new ResourceNotFound("Project not found: " + cohortDto.getProjectId()));

    if (project.hasEmptyOrDifferentOwner(userId)) {
      throw new ForbiddenException("Not allowed");
    }

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

  public Set<String> executeCohort(long cohortId, Boolean allowUsageOutsideEu) {
    Optional<Cohort> cohort = cohortRepository.findById(cohortId);
    return cohortExecutor.execute(
        cohort.orElseThrow(() -> new BadRequestException("Cohort not found: " + cohortId)),
        allowUsageOutsideEu);
  }

  public Set<String> executeCohort(CohortDto cohort) {
    return cohortExecutor.execute(modelMapper.map(cohort, Cohort.class), null);
  }

  public long getCohortSize(long cohortId, Boolean allowUsageOutsideEu) {
    return executeCohort(cohortId, allowUsageOutsideEu).size();
  }

  public long getCohortGroupSize(
      CohortGroupDto cohortGroupDto, String userId, Boolean allowUsageOutsideEu) {
    userDetailsService.checkIsUserApproved(userId);

    CohortGroup cohortGroup = convertToCohortGroupEntity(cohortGroupDto);
    Set<String> ehrIds =
        cohortExecutor.executeGroup(cohortGroup, cohortGroup.getParameters(), allowUsageOutsideEu);
    if (ehrIds.size() < privacyProperties.getMinHits()) {
      throw new PrivacyException("Too few matches, results withheld for privacy reasons.");
    }
    return ehrIds.size();
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
      throw new PrivacyException("Too few matches, results withheld for privacy reasons.");
    }

    return determineTemplatesHits(ehrIds, requestDto.getTemplateIds());
  }

  private Map<String, Integer> determineTemplatesHits(
      Set<String> ehrIds, List<String> templateIds) {
    Map<String, Integer> hits = new HashMap<>();
    templateIds.forEach(
        templateId -> {
          ContainmentDto containmentDto = aqlEditorContainmentService.buildContainment(templateId);

          if (containmentDto != null && StringUtils.isNotEmpty(containmentDto.getArchetypeId())) {
            List<Policy> policies = new LinkedList<>();
            policies.add(EhrPolicy.builder().cohortEhrIds(ehrIds).build());
            policies.add(
                TemplatesPolicy.builder().templatesMap(Map.of(templateId, templateId)).build());

            AqlDto dto = createQuery(containmentDto.getArchetypeId());
            policyService.apply(dto, policies);

            Set<String> templateHits =
                ehrBaseService.retrieveEligiblePatientIds(
                    new AqlBinder().bind(dto).getLeft().buildAql());
            hits.put(templateId, templateHits != null ? templateHits.size() : 0);

          } else {
            throw new BadRequestException("Cannot find template: " + templateId);
          }
        });
    return hits;
  }

  private AqlDto createQuery(String archetypeId) {
    org.ehrbase.aql.dto.containment.ContainmentDto contains =
        new org.ehrbase.aql.dto.containment.ContainmentDto();
    contains.setArchetypeId(archetypeId);
    contains.setId(1);

    SelectFieldDto fieldDto = new SelectFieldDto();
    fieldDto.setContainmentId(1);
    fieldDto.setAqlPath(Strings.EMPTY);

    SelectDto select = new SelectDto();
    select.setStatement(List.of(fieldDto));

    EhrDto ehrDto = new EhrDto();
    ehrDto.setContainmentId(0);
    ehrDto.setIdentifier("e");

    AqlDto dto = new AqlDto();
    dto.setEhr(ehrDto);
    dto.setSelect(select);
    dto.setContains(contains);

    return dto;
  }

  public Cohort updateCohort(CohortDto cohortDto, Long cohortId, String userId) {
    userDetailsService.checkIsUserApproved(userId);

    Cohort cohortToEdit =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(() -> new ResourceNotFound("Cohort not found: " + cohortId));

    if (cohortToEdit.getProject().hasEmptyOrDifferentOwner(userId)) {
      throw new ForbiddenException("Not allowed");
    }

    cohortToEdit.setCohortGroup(convertToCohortGroupEntity(cohortDto.getCohortGroup()));
    cohortToEdit.setDescription(cohortDto.getDescription());
    cohortToEdit.setName(cohortDto.getName());
    return cohortRepository.save(cohortToEdit);
  }

  private CohortGroup convertToCohortGroupEntity(CohortGroupDto cohortGroupDto) {
    if (cohortGroupDto == null) {
      throw new BadRequestException("Cohort group cannot be empty");
    }

    CohortGroup cohortGroup = modelMapper.map(cohortGroupDto, CohortGroup.class);
    cohortGroup.setId(null);

    if (cohortGroupDto.isAql()) {
      if (cohortGroupDto.getQuery() != null && cohortGroupDto.getQuery().getId() != null) {

        if (!aqlService.existsById(cohortGroupDto.getQuery().getId())) {
          throw new BadRequestException(
              String.format("%s %s", "Invalid aql id:", cohortGroupDto.getQuery().getId()));
        }

      } else {
        throw new BadRequestException("Invalid cohort group. Aql missing.");
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
              .collect(Collectors.toSet()));
    }

    return cohortGroup;
  }
}
