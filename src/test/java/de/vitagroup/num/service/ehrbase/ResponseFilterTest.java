package de.vitagroup.num.service.ehrbase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResponseFilterTest {
  private ResponseFilter filter;

  private List<QueryResponseData> testResponses;

  @BeforeEach
  public void setup(){
    filter = new ResponseFilter();
    filter.initialize();
    QueryResponseData testResponse = new QueryResponseData();
    Map<String, String> col1 = Map.of("path","bericht/context/setting|terminology");
    Map<String,String> col2 = Map.of("path","covid-19-diagnose/context/start_time");
    Map<String, String> col3 = Map.of("path","covid-19-diagnose/territory|code");
    Map<String, String> col4 = Map.of("path","laborbefund/_uid");
    Map<String, String> col5 = Map.of("path","laborbefund/_ehrid");
    Map<String, String> col6 = Map.of("path","laborbefund/ehr_id");
    testResponse.setColumns(List.of(col1, col2, col3, col4, col5, col6));
    List<Object> row1 = List.of("r1c1", "r1c2", "r1c3", "r1c4", "r1c5", "r1c6");
    List<Object> row2 = List.of("r2c1", "r2c2", "r2c3", "r2c4", "r2c5", "r2c6");
    List<Object> row3 = List.of("r3c1", "r3c2", "r3c3", "r3c4", "r3c5", "r3c6");
    testResponse.setRows(List.of(row1, row2, row3));
    testResponses = List.of(testResponse, testResponse);
  }

  @Test
  public void shouldFilterNecessaryColumns(){
    List<QueryResponseData> output = filter.filterResponse(testResponses);
    assertEquals(1,output.get(0).getColumns().size());
    assertEquals(1,output.get(0).getRows().get(0).size());
    assertEquals(1,output.get(0).getRows().get(1).size());
    assertEquals(1,output.get(0).getRows().get(2).size());
    assertEquals("covid-19-diagnose/context/start_time", output.get(0).getColumns().get(0).get("path"));
    assertEquals("r1c2", output.get(0).getRows().get(0).get(0));
    assertEquals("r2c2", output.get(0).getRows().get(1).get(0));
    assertEquals("r3c2", output.get(0).getRows().get(2).get(0));
    assertEquals(1,output.get(1).getColumns().size());
    assertEquals(1,output.get(1).getRows().get(0).size());
    assertEquals(1,output.get(1).getRows().get(1).size());
    assertEquals(1,output.get(1).getRows().get(2).size());
    assertEquals("covid-19-diagnose/context/start_time", output.get(1).getColumns().get(0).get("path"));
    assertEquals("r1c2", output.get(1).getRows().get(0).get(0));
    assertEquals("r2c2", output.get(1).getRows().get(1).get(0));
    assertEquals("r3c2", output.get(1).getRows().get(2).get(0));
  }
}
