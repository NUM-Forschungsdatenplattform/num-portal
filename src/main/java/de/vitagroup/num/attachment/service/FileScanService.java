package de.vitagroup.num.attachment.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class FileScanService {

    private final ClamAVService clamAVService;

    public void virusScan(MultipartFile file) {

    }
}
