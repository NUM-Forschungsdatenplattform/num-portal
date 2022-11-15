package de.vitagroup.num.service.cohort;


import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.dto.CohortSizeDto;
import de.vitagroup.num.domain.dto.TemplateSizeRequestDto;
import de.vitagroup.num.service.UserDetailsService;
import java.util.Map;

/**
 * Makes sure overridden methods only executed by valid users.
 */
public class CohortServiceSecurityWrapper extends DelegatingCohortService {

  private final UserDetailsService userDetailsService;

  public CohortServiceSecurityWrapper(CohortService delegate, UserDetailsService userDetailsService) {
    super(delegate);
    this.userDetailsService = userDetailsService;
  }

  @Override
  public Cohort getCohort(Long cohortId, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    return super.getCohort(cohortId, userId);
  }

  @Override
  public CohortSizeDto getCohortGroupSizeWithDistribution(CohortGroupDto cohortGroupDto,
      String userId, Boolean allowUsageOutsideEu) {
    userDetailsService.checkIsUserApproved(userId);
    return super.getCohortGroupSizeWithDistribution(cohortGroupDto, userId, allowUsageOutsideEu);
  }

  @Override
  public Cohort updateCohort(CohortDto cohortDto, Long cohortId, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    return super.updateCohort(cohortDto, cohortId, userId);
  }

  @Override
  public Map<String, Integer> getSizePerTemplates(String userId,
      TemplateSizeRequestDto requestDto) {
    userDetailsService.checkIsUserApproved(userId);
    return super.getSizePerTemplates(userId, requestDto);
  }

  @Override
  public long getCohortGroupSize(CohortGroupDto cohortGroupDto, String userId,
      Boolean allowUsageOutsideEu) {
    userDetailsService.checkIsUserApproved(userId);
    return super.getCohortGroupSize(cohortGroupDto, userId, allowUsageOutsideEu);
  }

  @Override
  public Cohort createCohort(CohortDto cohortDto, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    return super.createCohort(cohortDto, userId);
  }
}
