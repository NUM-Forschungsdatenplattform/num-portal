package de.vitagroup.num.service.ehrbase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.response.openehr.QueryResponseData;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
//@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ResponseFilter {
  private HashSet<String> pathFilters;

  @PostConstruct
  public void initialize() {
    try {
      File pathResource = new ClassPathResource("resultfilters/pathfilters.txt").getFile();
      pathFilters = new HashSet<>(Files.readAllLines(pathResource.toPath()));
    } catch (IOException e) {
      log.error("Failed to read project data filters, can't filter results.");
    }
  }

  public List<QueryResponseData> filterResponse(List<QueryResponseData> queryResponseDataList) {
    if(pathFilters == null){
      return new ArrayList<>();
    }
    List<QueryResponseData> resultList = new ArrayList<>();
    for (QueryResponseData queryResponseData : queryResponseDataList) {
      List<Map<String, String>> filteredColumns = new ArrayList<>();
      List<List<Object>> filteredRows = new ArrayList<>();
      for (int i = 0; i < queryResponseData.getRows().size(); i++) {
        filteredRows.add(new ArrayList<>());
      }
      for (int c = 0; c < queryResponseData.getColumns().size(); c++) {
        Map<String, String> column = queryResponseData.getColumns().get(c);
        if (keepColumn(column)) {
          filteredColumns.add(column);
          for (int i = 0; i < queryResponseData.getRows().size(); i++) {
            filteredRows.get(i).add(queryResponseData.getRows().get(i).get(c));
          }
        }
      }
      queryResponseData.setColumns(filteredColumns);
      queryResponseData.setRows(filteredRows);
      resultList.add(queryResponseData);
    }
    return resultList;
  }

  private boolean keepColumn(Map<String, String> column) {
    return !pathFilters.contains(column.get("path"));
  }
}
