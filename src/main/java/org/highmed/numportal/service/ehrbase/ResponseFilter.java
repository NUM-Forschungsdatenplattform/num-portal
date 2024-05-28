package org.highmed.numportal.service.ehrbase;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
@Slf4j
public class ResponseFilter {
  private HashSet<String> pathFilters;
  private List<Pattern> regexpFilters;

  @PostConstruct
  public void initialize() {
    try {
      BufferedReader pathResource = new BufferedReader(new InputStreamReader(
              new ClassPathResource("resultfilters/pathfilters.txt").getInputStream(), StandardCharsets.UTF_8));
      pathFilters = new HashSet<>(pathResource.lines().toList());
      BufferedReader regexpResource = new BufferedReader(new InputStreamReader(
              new ClassPathResource("resultfilters/regexpfilters.txt").getInputStream(), StandardCharsets.UTF_8));
      regexpFilters = regexpResource.lines().map(Pattern::compile).collect(
              Collectors.toList());
    } catch (IOException e) {
      log.error("Failed to read project data filters, can't filter results.");
    }
  }

  public List<QueryResponseData> filterResponse(List<QueryResponseData> queryResponseDataList) {
    if(pathFilters == null){
      return queryResponseDataList;
    }
    List<QueryResponseData> resultList = new ArrayList<>();
    for (QueryResponseData queryResponseData : queryResponseDataList) {
      List<Map<String, String>> filteredColumns = new ArrayList<>();
      List<List<Object>> filteredRows = new ArrayList<>();
      QueryResponseData filteredResponse = new QueryResponseData();
      if(isValidQueryResponseData(queryResponseData)) {
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
      }
      filteredResponse.setColumns(filteredColumns);
      filteredResponse.setRows(filteredRows);
      filteredResponse.setName(queryResponseData.getName());
      resultList.add(filteredResponse);
    }
    return resultList;
  }

  private boolean isValidQueryResponseData(QueryResponseData queryResponseData) {
    return queryResponseData.getRows() != null && queryResponseData.getColumns() != null;
  }

  private boolean keepColumn(Map<String, String> column) {
    String path = column.get("path");
    if(pathFilters.contains(path)){
      return false;
    }
    return regexpFilters.stream().filter(regexp -> regexp.matcher(path).matches()).findFirst().isEmpty();
  }
}
