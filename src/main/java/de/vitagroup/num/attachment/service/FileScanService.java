package de.vitagroup.num.attachment.service;

import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.SystemException;
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

    private final ClamAVService clamAVService;

    public void virusScan(MultipartFile file) {
        log.info("Start scanning file {}", file.getOriginalFilename());
        if (clamAVService.ping()) {
            try {
                String scanResult = clamAVService.scan(new ByteArrayInputStream(file.getBytes()));
                if (!clamAVService.isScannedFileSafe(scanResult)) {
                    log.error("File {} rejected by ClamAV", file.getOriginalFilename());
                    throw new BadRequestException(FileScanService.class, "File rejected", "File rejected");
                }
            } catch (IOException e) {
                log.error("Error occurred when scanning file {} ", file.getOriginalFilename(), e);
                throw new SystemException(FileScanService.class, "Could not scan file " + file.getOriginalFilename(),
                        "Could not scan file " + file.getOriginalFilename());
            }
        } else {
            log.error("ClamAV service did not respond to ping request");
            throw new SystemException(FileScanService.class, "Could not ping ClamAV service",
                    "Could not ping ClamAV service");
        }
        log.info("End scanning file {}", file.getOriginalFilename());
    }
}