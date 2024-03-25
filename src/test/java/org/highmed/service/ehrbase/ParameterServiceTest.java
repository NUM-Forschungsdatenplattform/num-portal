package org.highmed.service.ehrbase;

import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.junit.Assert;
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
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.highmed.domain.dto.ParameterOptionsDto;
import org.highmed.domain.model.admin.UserDetails;
import org.highmed.service.UserDetailsService;

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
    private static final String ARCHETYPE_ID_VALUE_MAGNITUDE = "openEHR-EHR-OBSERVATION.height.v2";

    private static final String ARCHETYPE_ID_VALUE_SYMBOL_VALUE = "openEHR-EHR-CLUSTER.laboratory_test_analyte.v1";
    private static final String ARCHETYPE_ID_VALUE = "openEHR-EHR-OBSERVATION.blood_pressure.v2";
    private static final String ARCHETYPE_ID_VALUE_UNITS = "openEHR-EHR-CLUSTER.laboratory_test_analyte.v1";

    private static final String ARCHETYPE_ID_VALUE_VALUE_ORDINAL = "openEHR-EHR-OBSERVATION.clinical_frailty_scale.v1";

    private static final String AQL_PATH_DEFINING_CODE = "/data[at0002]/items[at0019]/value/defining_code/code_string";
    private static final String AQL_PATH_VALUE_VALUE = "/items[at0001]/value/value";
    private static final String AQL_PATH_VALUE_MAGNITUDE = "/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude";
    private static final String AQL_PATH_VALUE_SYMBOL_VALUE = "/items[at0009]/value/symbol/value";
    private static final String AQL_PATH_VALUE = "/data[at0001]/events[at0006]/time/value";
    private static final String AQL_PATH_UNITS = "/items[at0001]/value/units";

    private static final String AQL_PATH_VALUE_VALUE_ORDINAL = "/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value";
    private static final String AQL_CODE_QUERY = "SELECT DISTINCT c0/data[at0002]/items[at0019] AS F1 FROM EHR e CONTAINS EVALUATION c0[openEHR-EHR-EVALUATION.gender.v1] ORDER BY c0/data[at0002]/items[at0019] ASC";
    private static final String AQL_MAGNITUDE_QUERY = "SELECT DISTINCT c0/data[at0001]/events[at0002]/data[at0003]/items[at0004] AS F1 FROM EHR e CONTAINS OBSERVATION c0[openEHR-EHR-OBSERVATION.height.v2] ORDER BY c0/data[at0001]/events[at0002]/data[at0003]/items[at0004] ASC";
    private static final String AQL_VALUE_UNITS_QUERY = "SELECT DISTINCT c0/items[at0001] AS F1 FROM EHR e CONTAINS CLUSTER c0[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1] ORDER BY c0/items[at0001] ASC";
    private static final String AQL_VALUE_VALUE_QUERY = "SELECT DISTINCT c0/items[at0001] AS F1 FROM EHR e CONTAINS CLUSTER c0[openEHR-EHR-CLUSTER.person_birth_data_iso.v0] ORDER BY c0/items[at0001] ASC";
    private static final String AQL_VALUE_QUERY = "SELECT DISTINCT c0/data[at0001]/events[at0006]/time AS F1 FROM EHR e CONTAINS OBSERVATION c0[openEHR-EHR-OBSERVATION.blood_pressure.v2] ORDER BY c0/data[at0001]/events[at0006]/time ASC";
    private static final String AQL_VALUE_VALUE_ORDINAL_QUERY = "SELECT DISTINCT c0/data[at0001]/events[at0002]/data[at0003]/items[at0004] AS F1 FROM EHR e CONTAINS OBSERVATION c0[openEHR-EHR-OBSERVATION.clinical_frailty_scale.v1] ORDER BY c0/data[at0001]/events[at0002]/data[at0003]/items[at0004] ASC";
    private static final String AQL_VALUE_SYMBOL_QUERY = "SELECT DISTINCT c0/items[at0009] AS F1 FROM EHR e CONTAINS CLUSTER c0[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1] ORDER BY c0/items[at0009] ASC";

    @Parameterized.Parameter()
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
        setupCodedTextResponseData();
        setupMagnitudeResponseData();
        setupValueUnitsResponseData();
        setupValueValueQuestionnaireResponse();
        setupValueQuestionnaireResponseData();
        setupValueOrdinalResponseData();
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setRows(new ArrayList<>());
        queryResponseData.setColumns(new ArrayList<>());
        Mockito.when(ehrBaseService.executePlainQuery(Mockito.eq(AQL_VALUE_SYMBOL_QUERY)))
                .thenReturn(queryResponseData);
        parameterService.getParameterValues("approvedUserId", aqlPath, arhcetypeId);
    }

    private void setupCodedTextResponseData() {
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
        queryResponseData.setQuery(AQL_CODE_QUERY);
        Mockito.when(ehrBaseService.executePlainQuery(Mockito.eq(AQL_CODE_QUERY))).thenReturn(queryResponseData);
    }
    private void setupMagnitudeResponseData() {
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setQuery(AQL_MAGNITUDE_QUERY);
        queryResponseData.setColumns(new ArrayList<>(List.of(Map.of("path", "/data[at0001]/events[at0002]/data[at0003]/items[at0004]"), Map.of("name", "F1"))));
        LinkedHashMap e1= new LinkedHashMap();
        e1.put("name", Map.of("value", "Größe/Länge", "_type", "DV_TEXT"));
        e1.put("value",Map.of("_type", "DV_QUANTITY",
                "units", "cm",
                "magnitude", 178));
        e1.put("_type", "ELEMENT");
        e1.put("archetype_node_id", "at0004");
        queryResponseData.setRows( List.of(
                new ArrayList<>(List.of(e1))));
        Mockito.when(ehrBaseService.executePlainQuery(AQL_MAGNITUDE_QUERY)).thenReturn(queryResponseData);
    }

    private void setupValueUnitsResponseData() {
        LinkedHashMap e1= new LinkedHashMap();
        e1.put("name", Map.of("value", "Nachweis", "_type", "DV_TEXT"));
        e1.put("value",Map.of("_type", "DV_CODED_TEXT",
                "value", "Detected (qualifier value)",
                "defining_code", Map.of("_type", "CODE_PHRASE",
                        "code_string", "260373001",
                        "terminology_id", Map.of("_type", "TERMINOLOGY_ID",
                                "value","SNOMED Clinical Terms"))));
        e1.put("_type", "ELEMENT");
        e1.put("archetype_node_id", "at0001");
        LinkedHashMap e2= new LinkedHashMap();
        e2.put("name", Map.of("value", "Nachweis", "_type", "DV_TEXT"));
        e2.put("value",Map.of("_type", "DV_CODED_TEXT",
                "value", "Inconclusive (qualifier value)",
                "defining_code", Map.of("_type", "CODE_PHRASE", "code_string", "419984006",
                        "terminology_id", Map.of("_type", "TERMINOLOGY_ID",
                                "value","SNOMED Clinical Terms"))));
        e2.put("_type", "ELEMENT");
        e2.put("archetype_node_id", "at0001");


        LinkedHashMap e3= new LinkedHashMap();
        e3.put("name", Map.of("value", "Messwert", "_type", "DV_TEXT"));
        e3.put("value",Map.of("_type", "DV_QUANTITY",
                "units", "µmol/l",
                "magnitude", 72));
        e3.put("_type", "ELEMENT");
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setColumns(new ArrayList<>(List.of(Map.of("path", "/ehr_id/value"), Map.of("uuid", "c/uuid"))));
        queryResponseData.setRows( List.of(
                new ArrayList<>(List.of(e1)),
                new ArrayList<>(List.of(e2)),
                new ArrayList<>(List.of(e3))));
        queryResponseData.setQuery(AQL_VALUE_UNITS_QUERY);
        Mockito.when(ehrBaseService.executePlainQuery(Mockito.eq(AQL_VALUE_UNITS_QUERY))).thenReturn(queryResponseData);
    }

    private void setupValueValueQuestionnaireResponse() {
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setQuery(AQL_VALUE_VALUE_QUERY);
        queryResponseData.setColumns(new ArrayList<>(List.of(Map.of("path", "/items[at0001]"), Map.of("name", "F1"))));
        LinkedHashMap e1= new LinkedHashMap();
        e1.put("name", Map.of("value", "Geburtsdatum", "_type", "DV_TEXT"));
        e1.put("value",Map.of("_type", "DV_DATE",
                "value", "1953-03-18"));
        e1.put("_type", "ELEMENT");
        e1.put("archetype_node_id", "at0001");
        queryResponseData.setRows( List.of(
                new ArrayList<>(List.of(e1))));
        Mockito.when(ehrBaseService.executePlainQuery(AQL_VALUE_VALUE_QUERY)).thenReturn(queryResponseData);
    }

    private void setupValueQuestionnaireResponseData() {
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setQuery(AQL_VALUE_QUERY);
        queryResponseData.setColumns(new ArrayList<>(List.of(Map.of("path", "/data[at0001]/events[at0006]/time"), Map.of("name", "F1"))));
        LinkedHashMap e1= new LinkedHashMap();
        e1.put("value","2012-09-30T00:00:00Z");
        e1.put("_type", "DV_DATE_TIME");
        queryResponseData.setRows( List.of(
                new ArrayList<>(List.of(e1))));
        Mockito.when(ehrBaseService.executePlainQuery(AQL_VALUE_QUERY)).thenReturn(queryResponseData);
    }

    private void setupValueOrdinalResponseData() {
        LinkedHashMap e1= new LinkedHashMap();
        e1.put("name", Map.of("value", "Beurteilung",
                              "_type", "DV_TEXT"));
        e1.put("value",Map.of("_type", "DV_ORDINAL",
                "value", 2.0,
                "symbol", Map.of("_type", "DV_CODED_TEXT",
                        "value", "2",
                        "defining_code", Map.of("_type", "CODE_PHRASE",
                                "code_string", "at0006",
                                "terminology_id", Map.of("_type", "TERMINOLOGY_ID",
                                                             "value","local")))));
        e1.put("_type", "ELEMENT");
        e1.put("archetype_node_id", "at0004");
        LinkedHashMap e2= new LinkedHashMap();
        e2.put("name", Map.of("value", "Beurteilung",
                "_type", "DV_TEXT"));
        e2.put("value",Map.of("_type", "DV_ORDINAL",
                "value", 3.0,
                "symbol", Map.of("_type", "DV_CODED_TEXT",
                        "value", "3",
                        "defining_code", Map.of("_type", "CODE_PHRASE",
                                "code_string", "at0007",
                                "terminology_id", Map.of("_type", "TERMINOLOGY_ID",
                                        "value","local")))));
        e2.put("_type", "ELEMENT");
        e2.put("archetype_node_id", "at0004");
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setColumns(new ArrayList<>(List.of(Map.of("path", "/data[at0001]/events[at0002]/data[at0003]/items[at0004]"), Map.of("uuid", "c/uuid"))));
        queryResponseData.setRows( List.of(
                new ArrayList<>(List.of(e1)),
                new ArrayList<>(List.of(e2))));
        queryResponseData.setQuery(AQL_PATH_VALUE_VALUE_ORDINAL);
        Mockito.when(ehrBaseService.executePlainQuery(Mockito.eq(AQL_VALUE_VALUE_ORDINAL_QUERY))).thenReturn(queryResponseData);
    }

    @Parameterized.Parameters
    public static Collection testInput() {
        return Arrays.asList(new Object[][]{
                {AQL_PATH_DEFINING_CODE, ARCHETYPE_ID_CODE},
                {AQL_PATH_VALUE_MAGNITUDE, ARCHETYPE_ID_VALUE_MAGNITUDE},
                {AQL_PATH_VALUE_VALUE, ARCHETYPE_ID_VALUE_VALUE},
                 {AQL_PATH_VALUE, ARCHETYPE_ID_VALUE},
                {AQL_PATH_VALUE_SYMBOL_VALUE, ARCHETYPE_ID_VALUE_SYMBOL_VALUE},
                {AQL_PATH_UNITS, ARCHETYPE_ID_VALUE_UNITS},
                {AQL_PATH_VALUE_VALUE_ORDINAL, ARCHETYPE_ID_VALUE_VALUE_ORDINAL}
        });
    }

    @Test
    public void evictParametersCache() {
        ConcurrentMapCache paramsCache = new ConcurrentMapCache("aqlParameters", false);
        paramsCache.putIfAbsent("aqlPath1", ParameterOptionsDto.builder().aqlPath("aqlPath1").type("DV_BOOLEAN").build());
        paramsCache.putIfAbsent("aqlPath2", ParameterOptionsDto.builder().aqlPath("aqlPath2").type("DV_ORDINAL").build());
        Mockito.when(cacheManager.getCache("aqlParameters")).thenReturn(paramsCache);
        parameterService.evictParametersCache();
        Assert.assertTrue(paramsCache.getNativeCache().isEmpty());
    }
}
