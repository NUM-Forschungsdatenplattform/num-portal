package org.highmed.numportal.service.util;

import org.highmed.numportal.domain.model.ExportType;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@AllArgsConstructor
@Component
public class ExportHeaderUtil {

  private static final String ZIP_FILE_ENDING = ".zip";
  private static final String JSON_FILE_ENDING = ".json";
  private static final String ZIP_MEDIA_TYPE = "application/zip";

  private final ExportUtil exportUtil;

  public MultiValueMap<String, String> getExportHeaders(ExportType format, Long projectId) {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    String fileEnding;
    if (format == ExportType.json) {
      fileEnding = JSON_FILE_ENDING;
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    } else {
      fileEnding = ZIP_FILE_ENDING;
      headers.add(HttpHeaders.CONTENT_TYPE, ZIP_MEDIA_TYPE);
    }
    headers.add(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=" + exportUtil.getExportFilenameBody(projectId) + fileEnding);
    return headers;
  }
}
