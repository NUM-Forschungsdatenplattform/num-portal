package org.highmed.numportal.service.policy;

import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.highmed.numportal.service.policy.EuropeanConsentPolicy;
import org.highmed.numportal.service.policy.ProjectPolicyService;
import org.highmed.numportal.service.policy.TemplatesPolicy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ProjectPolicyServiceTest {

    @InjectMocks
    private ProjectPolicyService projectPolicyService;

    private static final String Q1 = "Select e/ehr_id/value, " +
            "  c0/category/defining_code as category__defining_code " +
            "from EHR e" +
            "  contains COMPOSITION c0[openEHR-EHR-COMPOSITION.registereintrag.v1] " +
            "  contains OBSERVATION o1[openEHR-EHR-OBSERVATION.blood_pressure.v2] ";

    @Test
    public void applyTest() {
        AqlQuery aqlDto = AqlQueryParser.parse(Q1);
        Assert.assertNull(aqlDto.getWhere());
        projectPolicyService.apply(aqlDto, List.of(
                TemplatesPolicy.builder()
                        .templatesMap(Map.of("Blutdruck", "Blutdruck"))
                        .build(),
                EuropeanConsentPolicy.builder().oid("dummy-oid")
                        .build()));
        Assert.assertNotNull(aqlDto.getWhere());
        LogicalOperatorCondition where = (LogicalOperatorCondition)aqlDto.getWhere();
        Assert.assertEquals(2, where.getValues().size());
    }
}
