package de.vitagroup.num.service.policy;

import lombok.extern.slf4j.Slf4j;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.render.AqlRenderer;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProjectPolicyService {

    private static final String ERROR_MESSAGE = "Cannot parse aql query while logging";

    public void apply(AqlQuery aql, List<Policy> policies) {

        try {
            log.info(
                    String.format(
                            "[AQL QUERY] Aql before executing project policies: %s ",
                                AqlRenderer.render(aql)));
        } catch (Exception e) {
            log.error(ERROR_MESSAGE, e);
        }

        policies.forEach(policy -> policy.apply(aql));

        try {
            log.info(
                    String.format(
                            "[AQL QUERY] Aql after executing project policies: %s ",
                            AqlRenderer.render(aql)));
        } catch (Exception e) {
            log.error(ERROR_MESSAGE, e);
        }
    }
}
