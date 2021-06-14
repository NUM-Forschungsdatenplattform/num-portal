package de.vitagroup.num.service.policy;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.aql.binder.AqlBinder;
import org.ehrbase.aql.dto.AqlDto;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectPolicyService {

  private static final String ERROR_MESSAGE = "Cannot parse aql query while logging";

  public void apply(AqlDto aql, List<Policy> policies) {

    try {
      log.info(
          String.format(
              "[AQL QUERY] Aql before executing project policies: %s ",
              new AqlBinder().bind(aql).getLeft().buildAql()));
    } catch (Exception e) {
      log.error(ERROR_MESSAGE, e);
    }

    policies.forEach(policy -> policy.apply(aql));

    try {
      log.info(
          String.format(
              "[AQL QUERY] Aql after executing project policies: %s ",
              new AqlBinder().bind(aql).getLeft().buildAql()));
    } catch (Exception e) {
      log.error(ERROR_MESSAGE, e);
    }
  }
}
