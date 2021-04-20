package de.vitagroup.num.service.policy;

import java.util.List;
import org.ehrbase.aql.dto.AqlDto;
import org.springframework.stereotype.Service;

@Service
public class ProjectPolicyService {

  public void apply(AqlDto aql, List<Policy> policies) {
    policies.forEach(policy -> policy.apply(aql));
  }
}
