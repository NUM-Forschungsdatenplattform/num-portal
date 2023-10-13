package de.vitagroup.num.attachment.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RunWith(MockitoJUnitRunner.class)
public class FileScanServiceTest {

    @Mock
    private ClamAVService clamAVService;

    @InjectMocks
    private FileScanService fileScanService;

    @Test
    public void virusScanTest() throws IOException {
        InputStream fileContent = this.getClass().getResourceAsStream("/clamav/num-setup-db.pdf");
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", fileContent.readAllBytes());
        final String scanResponse = "stream: OK";
        Mockito.when(clamAVService.ping()).thenReturn(true);
        Mockito.when(clamAVService.scan(Mockito.any(InputStream.class))).thenReturn(scanResponse);
        Mockito.when(clamAVService.isScannedFileSafe(Mockito.eq(scanResponse))).thenReturn(true);
        fileScanService.virusScan(mockFile);
    }
}
