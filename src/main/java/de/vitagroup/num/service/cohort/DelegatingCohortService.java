package de.vitagroup.num.service.cohort;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class DelegatingCohortService implements CohortService{

  @Delegate
  private final CohortService delegate;
}
