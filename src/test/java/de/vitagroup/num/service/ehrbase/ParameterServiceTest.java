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

    ///items[at0002]/value/defining_code/code_string
    // /data[at0002]/items[at0022]/value/defining_code/code_string
    private static final String AQL_PATH_DEFINING_CODE = "/data[at0002]/items[at0019]/value/defining_code/code_string";
    private static final String AQL_PATH_VALUE_VALUE = "/items[at0001]/value/value";
    private static final String AQL_PATH_VALUE_MAGNITUDE = "/items[at0001]/value/magnitude";

    private static final String AQL_PATH_VALUE_SYMBOL_VALUE = "/items[at0009]/value/symbol/value";

    private static final String AQL_PATH_VALUE = "/data[at0001]/events[at0006]/time/value";

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

//        QueryResponseData response = new QueryResponseData();
//
//        response.setColumns(new ArrayList<>(List.of(Map.of("path", "/ehr_id/value"), Map.of("uuid", "c/uuid"))));
//        response.setRows(  List.of(
//                new ArrayList<>(List.of("testehrid1", Map.of("_type", "OBSERVATION", "uuid", "12345"))),
//                new ArrayList<>(List.of("testehrid2", Map.of("_type", "SECTION", "uuid", "bla")))));
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setColumns(new ArrayList<>(List.of(Map.of("path", "/ehr_id/value"), Map.of("uuid", "c/uuid"))));
        queryResponseData.setRows(  List.of(
                new ArrayList<>(List.of("name", Map.of("value", "Geschlecht bei der Geburt", "_type", "DV_TEXT"))),
                new ArrayList<>(List.of("value", Map.of("_type", "DV_CODED_TEXT", "value", "Male")))));
        Mockito.when(ehrBaseService.executePlainQuery(Mockito.anyString())).thenReturn(queryResponseData);
        parameterService.getParameterValues("approvedUserId", aqlPath, arhcetypeId);
    }

    @Parameterized.Parameters
    public static Collection testInput() {
        return Arrays.asList(new Object[][]{{AQL_PATH_DEFINING_CODE, ARCHETYPE_ID_CODE},
                {AQL_PATH_VALUE_MAGNITUDE, ARCHETYPE_ID_VALUE_MAGNITUDE},
                {AQL_PATH_VALUE_VALUE, ARCHETYPE_ID_VALUE_VALUE},
                {AQL_PATH_VALUE, ARCHETYPE_ID_VALUE},
                {AQL_PATH_VALUE_SYMBOL_VALUE, ARCHETYPE_ID_VALUE_SYMBOL_VALUE}
        });
    }
}
