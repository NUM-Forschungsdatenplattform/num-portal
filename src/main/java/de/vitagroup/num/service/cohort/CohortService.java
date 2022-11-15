package de.vitagroup.num.service.cohort;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.dto.CohortSizeDto;
import de.vitagroup.num.domain.dto.TemplateSizeRequestDto;
import java.util.Map;
import java.util.Set;

public interface CohortService {

  Cohort getCohort(Long cohortId, String userId);

  Cohort createCohort(CohortDto cohortDto, String userId);

  Cohort toCohort(CohortDto cohortDto);

  Set<String> executeCohort(long cohortId, Boolean allowUsageOutsideEu);

  Set<String> executeCohort(Cohort cohort, Boolean allowUsageOutsideEu);

  long getCohortGroupSize(
      CohortGroupDto cohortGroupDto, String userId, Boolean allowUsageOutsideEu);

  int getRoundedSize(long size);

  Map<String, Integer> getSizePerTemplates(
      String userId, TemplateSizeRequestDto requestDto);

  Cohort updateCohort(CohortDto cohortDto, Long cohortId, String userId);

  CohortSizeDto getCohortGroupSizeWithDistribution(
      CohortGroupDto cohortGroupDto, String userId, Boolean allowUsageOutsideEu);
}
