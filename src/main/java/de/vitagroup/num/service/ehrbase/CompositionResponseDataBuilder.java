package de.vitagroup.num.service.ehrbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.composition.Composition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.ehrbase.response.openehr.QueryResponseData;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CompositionResponseDataBuilder {
  private final ObjectMapper mapper;
  private final CompositionFlattener compositionFlattener;

  private static final String COLUMN_NAME = "name";
  private static final String COLUMN_PATH = "path";

  public List<QueryResponseData> build(List<Map<String, Object>> compositions) {
    List<Map<String, Object>> flatCompositionsList = createCompositionsMap(compositions);
    List<QueryResponseData> responseData = new LinkedList<>();

    flatCompositionsList.forEach(
        flatComposition -> {
          responseData.add(buildQueryResponseData(flatComposition));
        });
    return responseData;
  }

  private QueryResponseData buildQueryResponseData(Map<String, Object> composition) {
    List<Map<String, String>> columns = new ArrayList<>();
    List<Object> row = new ArrayList<>();

    composition.forEach(
        (k, v) -> {
          Map<String, String> header = new HashMap<>();
          header.put(COLUMN_NAME, k);
          header.put(COLUMN_PATH, k);
          columns.add(header);
          row.add(v);
        });

    List<List<Object>> rows = List.of(row);

    QueryResponseData queryResponseData = new QueryResponseData();
    queryResponseData.setColumns(columns);
    queryResponseData.setRows(rows);

    return queryResponseData;
  }

  public List<Map<String, Object>> createCompositionsMap(List<Map<String, Object>> compositions) {
    List<Map<String, Object>> compositionsMap = new ArrayList<>();

    compositions.forEach(
        compositionMap -> {
          String compositionString;
          try {
            compositionString = mapper.writeValueAsString(compositionMap);
            Composition composition =
                new CanonicalJson().unmarshal(compositionString, Composition.class);
            compositionsMap.add(compositionFlattener.flatten(composition));
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
        });

    return compositionsMap;
  }
}
