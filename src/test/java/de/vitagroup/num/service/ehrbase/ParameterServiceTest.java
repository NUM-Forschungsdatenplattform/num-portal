package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.service.UserDetailsService;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.CacheManager;

import java.util.ArrayList;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ParameterServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private EhrBaseService ehrBaseService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private ParameterService parameterService;

    private static final String archetypeId = "openEHR-EHR-EVALUATION.gender.v1";

    private static final String aqlPath = "/data[at0002]/items[at0019]/value/defining_code/code_string";

    @Before
    public void setup() {
        UserDetails approvedUser =
                UserDetails.builder().userId("approvedUserId").approved(true).build();
        when(userDetailsService.checkIsUserApproved("approvedUserId")).thenReturn(approvedUser);
    }

    @Test
    public void getParameterValuesTest() {
        QueryResponseData queryResponseData = new QueryResponseData();
        queryResponseData.setColumns(new ArrayList<>());
        queryResponseData.setRows(new ArrayList<>());
        Mockito.when(ehrBaseService.executePlainQuery(Mockito.anyString())).thenReturn(queryResponseData);
        parameterService.getParameterValues("approvedUserId", aqlPath, archetypeId);
    }
}
