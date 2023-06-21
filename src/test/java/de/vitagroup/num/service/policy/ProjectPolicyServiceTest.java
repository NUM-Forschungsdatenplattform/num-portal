package de.vitagroup.num.service.policy;

import org.ehrbase.aql.dto.AqlDto;
import org.ehrbase.aql.dto.condition.ConditionLogicalOperatorDto;
import org.ehrbase.aql.parser.AqlToDtoParser;
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
        AqlDto aqlDto = new AqlToDtoParser().parse(Q1);
        Assert.assertNull(aqlDto.getWhere());
        projectPolicyService.apply(aqlDto, List.of(
                TemplatesPolicy.builder()
                        .templatesMap(Map.of("Blutdruck", "Blutdruck"))
                        .build(),
                EuropeanConsentPolicy.builder().oid("dummy-oid")
                        .build()));
        Assert.assertNotNull(aqlDto.getWhere());
        ConditionLogicalOperatorDto where = (ConditionLogicalOperatorDto) aqlDto.getWhere();
        Assert.assertEquals(2, where.getValues().size());
    }
}
