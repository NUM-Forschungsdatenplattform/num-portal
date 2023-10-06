package de.vitagroup.num.attachment.service;

import de.vitagroup.num.properties.ClamAVProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@AllArgsConstructor
@Component
public class ClamAVService {

    private final ClamAVProperties clamAVProperties;

    private static final int CHUNK_SIZE = 2048;

    public boolean ping() {
        try(Socket s = new Socket()) {
            s.connect(new InetSocketAddress(clamAVProperties.getHost(),clamAVProperties.getPort()), clamAVProperties.getPingTimeout());
            log.info("ClamAV is up");
            return true;
        } catch (IOException e) {
            log.error("Failed to ping ClamAV at {}:{}", clamAVProperties.getHost(), clamAVProperties.getPort(), e);
            return false;
        }
    }

    public void scan(InputStream inputStream) {
        try(Socket socket = new Socket(clamAVProperties.getHost(), clamAVProperties.getPort())){
            socket.setSoTimeout(clamAVProperties.getTimeout());

            try(OutputStream out = new BufferedOutputStream(socket.getOutputStream())){

                out.write("zINSTREAM\0".getBytes(StandardCharsets.UTF_8));
                out.flush();
                log.info("Socket information = {} connected = {} ", socket, socket.isConnected());

                byte[] buffer = new byte[CHUNK_SIZE];
                try(InputStream ins = socket.getInputStream()) {
                    int read = inputStream.read(buffer);

//                    out.write(inputStream.readAllBytes());
//                    out.flush();
                    while (read >= 0) {
                        byte[] chunkSize = ByteBuffer.allocate(4).putInt(read).array();
                        out.write(chunkSize);
                        out.write(buffer, 0, read);

                        if(ins.available() > 0) {
                            byte[] reply = IOUtils.toByteArray(ins);
                            throw new IOException(
                                    "Reply from server: " + new String(reply, StandardCharsets.UTF_8));
                        }
                        read = inputStream.read(buffer);
                    }
                    out.write(new byte[] {0, 0, 0, 0});
                    out.flush();

                    String scanResult = new String(IOUtils.toByteArray(ins));
                    System.out.println(" ---scan result --- " + scanResult);
                }
            }

        } catch (IOException e) {
            log.error(" Error ", e);
            throw new RuntimeException(e);
        }
    }
}
