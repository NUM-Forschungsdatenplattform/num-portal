package de.vitagroup.num.service.executors;

import de.vitagroup.num.domain.*;
import de.vitagroup.num.service.MockEhrService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.vitagroup.num.service.exception.IllegalArgumentException;

@Slf4j
@Service
@AllArgsConstructor
public class PhenotypeExecutor {

    private final SetOperations setOperations;
    private final MockEhrService mockEhrService;

    public Set<String> execute(Phenotype phenotype) {

        if (phenotype == null || phenotype.getQuery() == null) {
            throw new IllegalArgumentException("Cannot execute an empty phenotype");
        }

        return execute(phenotype.getQuery());
    }

    private Set<String> execute(Expression expression) {
        Set<String> all = getAllPatientIds();

        if (expression instanceof GroupExpression) {
            GroupExpression groupExpression = (GroupExpression) expression;
            List<Set<String>> sets = groupExpression.getChildren().stream().map(this::execute).collect(Collectors.toList());

            return setOperations.apply(groupExpression.getOperator(), sets, all);

        } else if (expression instanceof AqlExpression) {

            AqlExpression aqlExpression = (AqlExpression) expression;

            return executeAql(aqlExpression.getAql());
        }
        return SetUtils.emptySet();
    }

    //TODO: implement call to the service responsible for querying open ehr for all patient ids;
    // service should cache patient ids per cohort execution
    private Set<String> getAllPatientIds() {
        return mockEhrService.getAllPatientIds();
    }


    //TODO: implement call to the service responsible for executing aqls
    private Set<String> executeAql(Aql aql) {
        return mockEhrService.executeAql(aql);
    }

}
