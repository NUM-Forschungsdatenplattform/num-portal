package org.highmed.numportal.attachment.service;

import org.highmed.numportal.domain.templates.ExceptionsTemplate;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.SystemException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@AllArgsConstructor
@Slf4j
public class FileScanService {

  private final ClamAVService clamAvService;

  public void virusScan(MultipartFile file) {
    log.info("Start scanning file {}", file.getOriginalFilename());
    if (clamAvService.ping()) {
      try {
        String scanResult = clamAvService.scan(new ByteArrayInputStream(file.getBytes()));
        if (!clamAvService.isScannedFileSafe(scanResult)) {
          log.error("File {} rejected by ClamAV", file.getOriginalFilename());
          throw new BadRequestException(FileScanService.class, "File rejected", "File rejected");
        }
      } catch (IOException e) {
        log.error("Error occurred when scanning file {} ", file.getOriginalFilename(), e);
        throw new SystemException(FileScanService.class, ExceptionsTemplate.CLAMAV_SCAN_FAILED,
            String.format(ExceptionsTemplate.CLAMAV_SCAN_FAILED, file.getOriginalFilename()));
      }
    } else {
      log.error("ClamAV service did not respond to ping request");
      throw new SystemException(FileScanService.class, ExceptionsTemplate.CLAMAV_PING_FAILED,
          ExceptionsTemplate.CLAMAV_PING_FAILED);
    }
    log.info("End scanning file {}", file.getOriginalFilename());
  }
}
