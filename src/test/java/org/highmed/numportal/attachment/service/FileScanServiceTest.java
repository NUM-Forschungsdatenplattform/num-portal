package org.highmed.numportal.attachment.service;

import org.highmed.numportal.attachment.service.ClamAVService;
import org.highmed.numportal.attachment.service.FileScanService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.SystemException;

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

    @Test(expected = SystemException.class)
    public void virusScanExceptionWhenPingTest() {
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "fileContent".getBytes());
        Mockito.when(clamAVService.ping()).thenReturn(false);
        fileScanService.virusScan(mockFile);
        Mockito.verify(clamAVService, Mockito.never()).scan(Mockito.any(InputStream.class));
        Mockito.verify(clamAVService, Mockito.never()).isScannedFileSafe(Mockito.any(String.class));
    }

    @Test(expected = SystemException.class)
    public void virusScanExceptionWhileScanningTest() {
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "fileContent".getBytes());
        Mockito.when(clamAVService.ping()).thenReturn(true);
        Mockito.when(clamAVService.scan(Mockito.any(InputStream.class))).thenAnswer((t)-> {throw new IOException();});
        fileScanService.virusScan(mockFile);
        Mockito.verify(clamAVService, Mockito.never()).isScannedFileSafe(Mockito.any(String.class));
    }

    @Test(expected = BadRequestException.class)
    public void virusScanMalwareFoundExceptionTest() {
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "fileContent".getBytes());
        Mockito.when(clamAVService.ping()).thenReturn(true);
        Mockito.when(clamAVService.scan(Mockito.any(InputStream.class))).thenReturn("file:ERROR");
        fileScanService.virusScan(mockFile);
    }
}
