package de.vitagroup.num.service.ehrbase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

@RunWith(MockitoJUnitRunner.class)
public class CompositionResponseDataBuilderTest {
  @Spy public ObjectMapper mapper;

  @Spy @InjectMocks CompositionResponseDataBuilder builder;

  @SneakyThrows
  @Test
  public void shouldCorrectlyComputeQueryResponseData() {
    doReturn(createCompositionsList()).when(builder).createCompositionsMap(any());

    QueryResponseData response = builder.build(List.of(Map.of()));
    String result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);

    assertThat(response, notNullValue());
    assertEquals(10, (response.getColumns().size()));
    assertEquals(3, response.getRows().size());
    assertEquals(
        IOUtils.toString(
            getClass().getResourceAsStream("/testdata/expected.json"), StandardCharsets.UTF_8),
        result);
  }

  private List<Map<String, String>> createCompositionsList() {
    Map composition1 =
        Map.of(
            "c1/k1", "c1v1",
            "c1/k2", "c1v2",
            "c1/k3", "c1v3",
            "c1/k4", "c1v4",
            "c1/k5", "c1v5",
            "c1/k6", "c1v6",
            "c1/only/in/composition/1", "only in composition one");

    Map composition2 =
        Map.of(
            "c1/k1", "c2v1",
            "c1/k2", "c2v2",
            "c1/k3", "c2v3",
            "c1/k4", "c2v4",
            "c1/k5", "c2v5",
            "c1/k6", "c2v6",
            "c1/only/in/composition/2/and/3", "only in composition two and three",
            "c1/only/in/composition/2", "only in composition two");

    Map composition3 =
        Map.of(
            "c1/k1", "c3v1",
            "c1/k2", "c3v2",
            "c1/k3", "c3v3",
            "c1/k4", "c3v4",
            "c1/k5", "c3v5",
            "c1/k6", "c3v6",
            "c1/only/in/composition/2/and/3", "only in composition two and three",
            "c1/only/in/composition/3", "only in composition three");

    return List.of(composition1, composition2, composition3);
  }
}
