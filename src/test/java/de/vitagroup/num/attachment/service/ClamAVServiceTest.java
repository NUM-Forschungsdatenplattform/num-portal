package de.vitagroup.num.attachment.service;

import de.vitagroup.num.properties.ClamAVProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RunWith(MockitoJUnitRunner.class)
public class ClamAVServiceTest {

    @Mock
    private ClamAVProperties clamAVProperties;

    @InjectMocks
    private ClamAVService clamAVService;

    @Before
    public void setup() {
        Mockito.when(clamAVProperties.getHost()).thenReturn("localhost");
        Mockito.when(clamAVProperties.getPort()).thenReturn(3310);
        //Mockito.when(clamAVProperties.getConnectionTimeout()).thenReturn(2000);
        Mockito.when(clamAVProperties.getReadTimeout()).thenReturn(15000);
    }

    @Test
    public void pingTest() {
        Assert.assertTrue(clamAVService.ping());
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
}
