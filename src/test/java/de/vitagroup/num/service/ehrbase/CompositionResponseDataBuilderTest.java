package de.vitagroup.num.service.ehrbase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CompositionResponseDataBuilderTest {
  @Spy public ObjectMapper mapper;

  @Spy @InjectMocks CompositionResponseDataBuilder builder;

  private static final Map COMP_1 =
      Map.of(
          "c1/k1", "c1v1",
          "c1/k2", "c1v2",
          "c1/k3", "c1v3",
          "c1/k4", "c1v4",
          "c1/k5", "c1v5",
          "c1/k6", "c1v6",
          "c1/only/in/composition/1", "only in composition one");

  private static final Map COMP_2 =
      Map.of(
          "c1/k1", "c2v1",
          "c1/k2", "c2v2",
          "c1/k3", "c2v3",
          "c1/k4", "c2v4",
          "c1/k5", "c2v5",
          "c1/k6", "c2v6",
          "c1/only/in/composition/2/and/3", "only in composition two and three",
          "c1/only/in/composition/2", "only in composition two");

  private static final Map COMP_3 =
      Map.of(
          "c1/k1", "c3v1",
          "c1/k2", "c3v2",
          "c1/k3", "c3v3",
          "c1/k4", "c3v4",
          "c1/k5", "c3v5",
          "c1/k6", "c3v6",
          "c1/only/in/composition/2/and/3", "only in composition two and three",
          "c1/only/in/composition/3", "only in composition three");

  private static final String PATH = "path";

  @SneakyThrows
  @Test
  public void shouldCorrectlyComputeQueryResponseData() {
    doReturn(createCompositionsList()).when(builder).createCompositionsMap(any());

    List<QueryResponseData> response = builder.build(List.of(Map.of()));

    assertThat(response, notNullValue());
    assertEquals(3, response.size());

    QueryResponseData one = response.get(0);
    assertEquals(1, one.getRows().size());
    assertEquals(7, one.getColumns().size());


    for (int i = 0; i < one.getRows().size(); i++) {
      Map<String, String> header = one.getColumns().get(i);
      Object cell = one.getRows().get(0).get(i);
      assertEquals(cell, COMP_1.get(header.get(PATH)));
    }

    QueryResponseData two = response.get(1);
    assertEquals(1, two.getRows().size());
    assertEquals(8, two.getColumns().size());


    for (int i = 0; i < two.getRows().size(); i++) {
      Map<String, String> header = two.getColumns().get(i);
      Object cell = two.getRows().get(0).get(i);
      assertEquals(cell, COMP_2.get(header.get(PATH)));
    }

    QueryResponseData three = response.get(2);
    assertEquals(1, three.getRows().size());
    assertEquals(8, three.getColumns().size());


    for (int i = 0; i < three.getRows().size(); i++) {
      Map<String, String> header = three.getColumns().get(i);
      Object cell = three.getRows().get(0).get(i);
      assertEquals(cell, COMP_3.get(header.get(PATH)));
    }
  }

  private List<Map<String, String>> createCompositionsList() {
    return List.of(COMP_1, COMP_2, COMP_3);
  }
}
