package org.highmed.attachment.service;

import org.highmed.integrationtesting.config.ClamAVContainer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.properties.ClamAVProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RunWith(MockitoJUnitRunner.class)
public class ClamAVServiceTest {

    @ClassRule
    public static final ClamAVContainer clamAVContainer = ClamAVContainer.getInstance();

    @Mock
    private ClamAVProperties clamAVProperties;

    @InjectMocks
    private ClamAVService clamAVService;

    @Before
    public void setup() {
        Mockito.when(clamAVProperties.getHost()).thenReturn(clamAVContainer.getHost());
        Mockito.when(clamAVProperties.getPort()).thenReturn(clamAVContainer.getMappedPort(3310));
        Mockito.when(clamAVProperties.getConnectionTimeout()).thenReturn(2000);
        Mockito.when(clamAVProperties.getReadTimeout()).thenReturn(15000);
    }

    @Test
    public void pingTest() {
        Assert.assertTrue(clamAVService.ping());
    }
    @Test
    public void pingFailedTest() {
        Mockito.when(clamAVProperties.getHost()).thenReturn("localhost");
        Mockito.when(clamAVProperties.getPort()).thenReturn(3311);
        Assert.assertFalse(clamAVService.ping());
    }
    @Test
    public void testInvalidFileContent() {
        // https://www.eicar.org/download-anti-malware-testfile/
        byte[] EICAR = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*".getBytes(StandardCharsets.UTF_8);
        String scanResult = clamAVService.scan(new ByteArrayInputStream(EICAR));
        Assert.assertFalse(clamAVService.isScannedFileSafe(scanResult));
    }

    @Test
    public void testValidFileContent() {
        InputStream fileContent = this.getClass().getResourceAsStream("/clamav/num-setup-db.pdf");
        String scanResult = clamAVService.scan(fileContent);
        Assert.assertTrue(clamAVService.isScannedFileSafe(scanResult));
    }
    @Test
    public void testStreamingChunks() {
        byte[] chunks = new byte[20000];
        String scanResult = clamAVService.scan(new ByteArrayInputStream(chunks));
        Assert.assertTrue(clamAVService.isScannedFileSafe(scanResult));
    }
}
