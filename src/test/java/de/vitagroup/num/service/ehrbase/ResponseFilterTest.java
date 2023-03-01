package de.vitagroup.num.service.ehrbase;

import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ResponseFilterTest {
  @InjectMocks
  @Spy
  private ResponseFilter filter;

  private static List<QueryResponseData> testResponses;

  @Before
  public void setup(){
    filter.initialize();
    QueryResponseData testResponse = new QueryResponseData();
    Map<String, String> col1 = Map.of("path","bericht/context/setting|terminology");
    Map<String,String> col2 = Map.of("path","covid-19-diagnose/context/start_time");
    Map<String, String> col3 = Map.of("path","covid-19-diagnose/territory|code");
    Map<String, String> col4 = Map.of("path","laborbefund/_uid");
    Map<String, String> col5 = Map.of("path","laborbefund/_ehrid");
    Map<String, String> col6 = Map.of("path","laborbefund/ehr_id");
    Map<String, String> col7 = Map.of("path","covid-19-diagnose/test|code");
    testResponse.setColumns(List.of(col1, col2, col3, col4, col5, col6,col7));
    List<Object> row1 = List.of("r1c1", "r1c2", "r1c3", "r1c4", "r1c5", "r1c6", "r1c7");
    List<Object> row2 = List.of("r2c1", "r2c2", "r2c3", "r2c4", "r2c5", "r2c6", "r2c7");
    List<Object> row3 = List.of("r3c1", "r3c2", "r3c3", "r3c4", "r3c5", "r3c6", "r3c7");
    testResponse.setRows(List.of(row1, row2, row3));
    testResponses = List.of(testResponse, testResponse);
  }

  @Test
  public void shouldFilterNecessaryColumns(){
    List<QueryResponseData> output = filter.filterResponse(testResponses);
    assertEquals(2,output.get(0).getColumns().size());
    assertEquals(2,output.get(0).getRows().get(0).size());
    assertEquals(2,output.get(0).getRows().get(1).size());
    assertEquals(2,output.get(0).getRows().get(2).size());
    assertEquals("covid-19-diagnose/context/start_time", output.get(0).getColumns().get(0).get("path"));
    assertEquals("r1c2", output.get(0).getRows().get(0).get(0));
    assertEquals("r2c2", output.get(0).getRows().get(1).get(0));
    assertEquals("r3c2", output.get(0).getRows().get(2).get(0));
    assertEquals("covid-19-diagnose/test|code", output.get(0).getColumns().get(1).get("path"));
    assertEquals("r1c7", output.get(0).getRows().get(0).get(1));
    assertEquals("r2c7", output.get(0).getRows().get(1).get(1));
    assertEquals("r3c7", output.get(0).getRows().get(2).get(1));
    assertEquals(2,output.get(1).getColumns().size());
    assertEquals(2,output.get(1).getRows().get(0).size());
    assertEquals(2,output.get(1).getRows().get(1).size());
    assertEquals(2,output.get(1).getRows().get(2).size());
    assertEquals("covid-19-diagnose/context/start_time", output.get(1).getColumns().get(0).get("path"));
    assertEquals("r1c2", output.get(1).getRows().get(0).get(0));
    assertEquals("r2c2", output.get(1).getRows().get(1).get(0));
    assertEquals("r3c2", output.get(1).getRows().get(2).get(0));
    assertEquals("covid-19-diagnose/test|code", output.get(1).getColumns().get(1).get("path"));
    assertEquals("r1c7", output.get(1).getRows().get(0).get(1));
    assertEquals("r2c7", output.get(1).getRows().get(1).get(1));
    assertEquals("r3c7", output.get(1).getRows().get(2).get(1));
  }
}
