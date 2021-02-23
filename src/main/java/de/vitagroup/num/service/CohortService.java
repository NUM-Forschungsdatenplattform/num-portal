package de.vitagroup.num.service;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.service.executors.CohortExecutor;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CohortService {

  private final CohortRepository cohortRepository;
  private final CohortExecutor cohortExecutor;
  private final UserDetailsService userDetailsService;
  private final ModelMapper modelMapper;
  private final PhenotypeService phenotypeService;
  private final StudyService studyService;

  public Cohort getCohort(Long cohortId) {
    return cohortRepository.findById(cohortId).orElseThrow(ResourceNotFound::new);
  }

  public Cohort createCohort(CohortDto cohortDto, String userId) {
    verifyUserAndGetUserDetails(userId);

    Study study =
        studyService
            .getStudyById(cohortDto.getStudyId())
            .orElseThrow(() -> new ResourceNotFound("Study not found: " + cohortDto.getStudyId()));

    if (study.hasEmptyOrDifferentOwner(userId)) {
      throw new ForbiddenException("Not allowed");
    }

    Cohort cohort =
        Cohort.builder()
            .name(cohortDto.getName())
            .description(cohortDto.getDescription())
            .study(study)
            .cohortGroup(convertToCohortGroupEntity(cohortDto.getCohortGroup(), userId))
            .build();

    study.setCohort(cohort);
    return cohortRepository.save(cohort);
  }

  public Set<String> executeCohort(long cohortId) {
    Optional<Cohort> cohort = cohortRepository.findById(cohortId);
    return cohortExecutor.execute(
        cohort.orElseThrow(() -> new BadRequestException("Cohort not found: " + cohortId)));
  }

  public long getCohortSize(long cohortId) {
    return executeCohort(cohortId).size();
  }

  public long getCohortGroupSize(CohortGroupDto cohortGroupDto, String userId) {
    UserDetails coordinator = verifyUserAndGetUserDetails(userId);

    CohortGroup cohortGroup = convertToCohortGroupEntity(cohortGroupDto, coordinator.getUserId());
    return cohortExecutor.executeGroup(cohortGroup).size();
  }

  public Cohort updateCohort(CohortDto cohortDto, Long cohortId, String userId) {
    verifyUserAndGetUserDetails(userId);

    Cohort cohortToEdit =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(() -> new ResourceNotFound("Cohort not found: " + cohortId));

    if (cohortToEdit.getStudy().hasEmptyOrDifferentOwner(userId)) {
      throw new ForbiddenException("Not allowed");
    }

    cohortToEdit.setCohortGroup(convertToCohortGroupEntity(cohortDto.getCohortGroup(), userId));
    cohortToEdit.setDescription(cohortDto.getDescription());
    cohortToEdit.setName(cohortDto.getName());
    return cohortRepository.save(cohortToEdit);
  }

  private CohortGroup convertToCohortGroupEntity(CohortGroupDto cohortGroupDto, String userId) {
    if (cohortGroupDto == null) {
      throw new BadRequestException("Cohort groups cannot be empty");
    }

    CohortGroup cohortGroup = modelMapper.map(cohortGroupDto, CohortGroup.class);
    cohortGroup.setId(null);
    if (cohortGroupDto.isPhenotype()) {
      Optional<Phenotype> phenotype =
          phenotypeService.getPhenotypeById(cohortGroupDto.getPhenotypeId());

      if (phenotype.isPresent()) {

        if (phenotype.get().hasEmptyOrDifferentOwner(userId)) {
          throw new ForbiddenException(
              "Cannot access phenotype: "
                  + phenotype.get().getName()
                  + ", phenotype has a different or missing owner");
        }

        cohortGroup.setPhenotype(phenotype.get());

      } else {
        throw new BadRequestException("Invalid phenotype id");
      }
    }

    if (cohortGroupDto.isGroup()) {
      cohortGroup.setChildren(
          cohortGroupDto.getChildren().stream()
              .map(
                  child -> {
                    CohortGroup cohortGroupChild = convertToCohortGroupEntity(child, userId);
                    cohortGroupChild.setParent(cohortGroup);
                    return cohortGroupChild;
                  })
              .collect(Collectors.toSet()));
    }

    return cohortGroup;
  }

  private UserDetails verifyUserAndGetUserDetails(String userId) {
    Optional<UserDetails> userDetails = userDetailsService.getUserDetailsById(userId);

    if (userDetails.isEmpty()) {
      throw new SystemException("Logged in user not found in portal");
    }

    if (userDetails.get().isNotApproved()) {
      throw new ForbiddenException("Logged in user not approved:" + userId);
    }
    return userDetails.get();
  }
}
