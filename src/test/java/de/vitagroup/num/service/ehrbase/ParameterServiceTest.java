package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.service.UserDetailsService;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.CacheManager;

import java.util.*;

import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ParameterServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private EhrBaseService ehrBaseService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private ParameterService parameterService;

    private static final String ARCHETYPE_ID_CODE = "openEHR-EHR-EVALUATION.gender.v1";
    private static final String ARCHETYPE_ID_VALUE_VALUE = "openEHR-EHR-CLUSTER.person_birth_data_iso.v0";
    private static final String ARCHETYPE_ID_VALUE_MAGNITUDE = "openEHR-EHR-CLUSTER.laboratory_test_analyte.v1";

    private static final String ARCHETYPE_ID_VALUE_SYMBOL_VALUE = "openEHR-EHR-CLUSTER.laboratory_test_analyte.v1";
    private static final String ARCHETYPE_ID_VALUE = "openEHR-EHR-OBSERVATION.blood_pressure.v2";
    private static final String ARCHETYPE_ID_VALUE_UNITS = "openEHR-EHR-OBSERVATION.blood_pressure.v2";

    ///items[at0002]/value/defining_code/code_string
    // /data[at0002]/items[at0022]/value/defining_code/code_string
    private static final String AQL_PATH_DEFINING_CODE = "/data[at0002]/items[at0019]/value/defining_code/code_string";
    private static final String AQL_PATH_VALUE_VALUE = "/items[at0001]/value/value";
    private static final String AQL_PATH_VALUE_MAGNITUDE = "/items[at0001]/value/magnitude";

    private static final String AQL_PATH_VALUE_SYMBOL_VALUE = "/items[at0009]/value/symbol/value";

    private static final String AQL_PATH_VALUE = "/data[at0001]/events[at0006]/time/value";

    private static final String AQL_PATH_UNITS = "/data[at0001]/events[at0006]/value/units";

    private static final String AQL_CODE_QUERY = "Select distinct e0/data[at0002]/items[at0019] as F1 from EHR e contains EVALUATION e0[openEHR-EHR-EVALUATION.gender.v1] order by e0/data[at0002]/items[at0019] ASCENDING";

    @Parameterized.Parameter(0)
    public String aqlPath;

    @Parameterized.Parameter(1)
    public String arhcetypeId;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        UserDetails approvedUser =
                UserDetails.builder().userId("approvedUserId").approved(true).build();
        when(userDetailsService.checkIsUserApproved("approvedUserId")).thenReturn(approvedUser);
    }

    @Test
    @MethodSource("testInput")
    public void getParameterValuesTest() {

        LinkedHashMap e1= new LinkedHashMap();
        e1.put("name", Map.of("value", "Geschlecht bei der Geburt", "_type", "DV_TEXT"));
        e1.put("value",Map.of("_type", "DV_CODED_TEXT",
                "value", "Male",
                "defining_code", Map.of("_type", "CODE_PHRASE",
                                            "code_string", "male",
                                            "terminology_id", Map.of("_type", "TERMINOLOGY_ID",
                                                                        "value","http://hl7.org/fhir/administrative-gender"))));
        e1.put("_type", "ELEMENT");
        e1.put("archetype_node_id", "at0019");
        LinkedHashMap e2= new LinkedHashMap();
        e2.put("name", Map.of("value", "Geschlecht bei der Geburt", "_type", "DV_TEXT"));
        e2.put("value",Map.of("_type", "DV_CODED_TEXT",
                              "value", "divers",
                              "defining_code", Map.of("_type", "CODE_PHRASE", "code_string", "D",
                                                          "terminology_id", Map.of("_type", "TERMINOLOGY_ID",
                                                                                      "value","http://hl7.org/fhir/administrative-gender"))));
        e2.put("_type", "ELEMENT");
        e2.put("archetype_node_id", "at0019");
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setColumns(new ArrayList<>(List.of(Map.of("path", "/ehr_id/value"), Map.of("uuid", "c/uuid"))));
        queryResponseData.setRows( List.of(
                new ArrayList<>(List.of(e1)),
                new ArrayList<>(List.of(e2))));
        Mockito.when(ehrBaseService.executePlainQuery(Mockito.anyString())).thenReturn(queryResponseData);
        parameterService.getParameterValues("approvedUserId", aqlPath, arhcetypeId);
    }

    @Parameterized.Parameters
    public static Collection testInput() {
        return Arrays.asList(new Object[][]{{AQL_PATH_DEFINING_CODE, ARCHETYPE_ID_CODE},
                {AQL_PATH_VALUE_MAGNITUDE, ARCHETYPE_ID_VALUE_MAGNITUDE},
                {AQL_PATH_VALUE_VALUE, ARCHETYPE_ID_VALUE_VALUE},
                {AQL_PATH_VALUE, ARCHETYPE_ID_VALUE},
                {AQL_PATH_VALUE_SYMBOL_VALUE, ARCHETYPE_ID_VALUE_SYMBOL_VALUE},
                {AQL_PATH_UNITS, ARCHETYPE_ID_VALUE}
        });
    }
}
