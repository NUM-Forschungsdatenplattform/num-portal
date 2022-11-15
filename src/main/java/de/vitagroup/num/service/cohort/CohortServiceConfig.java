package de.vitagroup.num.service.cohort;

import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.AqlService;
import de.vitagroup.num.service.ContentService;
import de.vitagroup.num.service.TemplateService;
import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.executors.CohortExecutor;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CohortServiceConfig {

  @Bean
  CohortService cohortService(
      UserDetailsService userDetailsService,
      CohortRepository cohortRepository,
      CohortExecutor cohortExecutor,
      ModelMapper modelMapper,
      AqlService aqlService,
      ProjectRepository projectRepository,
      PrivacyProperties privacyProperties,
      ProjectPolicyService projectPolicyService,
      EhrBaseService ehrBaseService,
      ContentService contentService,
      TemplateService templateService) {
    CohortService standard = new StandardCohortService(
        cohortRepository,
        cohortExecutor,
        modelMapper,
        aqlService,
        projectRepository,
        privacyProperties,
        projectPolicyService,
        ehrBaseService,
        contentService,
        templateService);
    return new CohortServiceSecurityWrapper(standard, userDetailsService);
  }
}
