package org.highmed.numportal.service;

import org.highmed.numportal.domain.dto.CohortDto;
import org.highmed.numportal.domain.model.ExportType;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.service.exception.SystemException;
import org.highmed.numportal.service.util.ExportUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManagerServiceTest {

  private static final String CORONA_TEMPLATE = "Corona_Anamnese";

  @Mock
  private CohortService cohortService;

  @InjectMocks
  private ExportUtil exportUtil;

  @InjectMocks
  private ManagerService managerService;

  @Spy
  private ObjectMapper mapper;

  @Test(expected = SystemException.class)
  public void executeManagerProjectSystemException() throws JsonProcessingException {
    CohortDto cohortDto = CohortDto.builder().name("Cohort name").id(2L).build();
    when(mapper.writeValueAsString(any(Object.class))).thenThrow(new JsonProcessingException("Error"){});
    managerService.executeManagerProject(cohortDto, Arrays.asList("1", "2"), "ownerCoordinatorId");
  }

  @Test
  public void shouldSuccessfullyExecuteManagerProject() {
    CohortDto cohortDto = CohortDto.builder().name("Cohort name").id(2L).build();

    UserDetails userDetails =
        UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();

    String result =
        managerService.executeManagerProject(
            cohortDto, List.of(CORONA_TEMPLATE), userDetails.getUserId());

    assertThat(result, is("[{\"name\":\"Corona_Anamnese\",\"columns\":null,\"rows\":null}]"));
  }

  @Test
  public void shouldHandleExecuteManagerProjectWithEmptyTemplates() {
    executeManagerProjectWithoutTemplates(Collections.EMPTY_LIST);
  }

  @Test
  public void shouldHandleExecuteManagerProjectWithNullTemplates() {
    executeManagerProjectWithoutTemplates(null);
  }

  private void executeManagerProjectWithoutTemplates(List<String> templates) {
    CohortDto cohortDto = CohortDto.builder().name("Cohort name").id(2L).build();
    UserDetails userDetails =
        UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();
    String result =
        managerService.executeManagerProject(
            cohortDto, templates, userDetails.getUserId());
    assertThat(result, is("[]"));
  }

  @Test
  public void streamResponseBody() throws IOException {
    QueryResponseData response = new QueryResponseData();
    response.setName("response-one");
    response.setColumns(new ArrayList<>(List.of(Map.of("path", "/ehr_id/value"), Map.of("uuid", "c/uuid"))));
    response.setRows(  List.of(
        new ArrayList<>(List.of("ehr-id-1", Map.of("_type", "OBSERVATION", "uuid", "12345"))),
        new ArrayList<>(List.of("ehr-id-2", Map.of("_type", "SECTION", "uuid", "bla")))));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    exportUtil.streamResponseAsZip(List.of(response), "testFile", out);

    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry expectedFile = zipInputStream.getNextEntry();
    Assert.assertEquals("testFile_response-one.csv", expectedFile.getName());
  }

  @Test
  public void getManagerExportResponseBodyTest() {
    CohortDto cohortDto = CohortDto.builder()
                                   .name("alter cohort")
                                   .projectId(2L).build();
    managerService.getManagerExportResponseBody(cohortDto, List.of("Alter"), "approvedCoordinatorId", ExportType.json);
    Mockito.verify(cohortService, Mockito.times(1)).toCohort(Mockito.any(CohortDto.class));
  }
}
