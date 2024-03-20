package org.highmed.attachment.service;

import org.highmed.properties.ClamAVProperties;
import org.highmed.service.exception.SystemException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@AllArgsConstructor
@Component
public class ClamAVService {

    private final ClamAVProperties clamAVProperties;

    private static final int CHUNK_SIZE = 4096;
    private static final String INSTREAM = "zINSTREAM\0";

    private static final String INSTREAM_SIZE_LIMIT_RESPONSE = "INSTREAM size limit exceeded.";

    public boolean ping() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(clamAVProperties.getHost(), clamAVProperties.getPort()), clamAVProperties.getConnectionTimeout());
            log.info("ClamAV is up");
            return true;
        } catch (IOException e) {
            log.error("Failed to ping ClamAV at {}:{}", clamAVProperties.getHost(), clamAVProperties.getPort(), e);
            return false;
        }
    }

    public String scan(InputStream fileContent) {
        try (Socket socket = new Socket(clamAVProperties.getHost(), clamAVProperties.getPort())) {
            socket.setSoTimeout(clamAVProperties.getReadTimeout());

            try (OutputStream out = new BufferedOutputStream(socket.getOutputStream())) {
                //send INSTREAM command
                out.write(INSTREAM.getBytes(StandardCharsets.UTF_8));
                out.flush();
                log.debug("Socket information = {} connected = {} ", socket, socket.isConnected());

                byte[] buffer = new byte[CHUNK_SIZE];
                try (InputStream ins = socket.getInputStream()) {
                    int read = fileContent.read(buffer);

                    while (read >= 0) {
                        byte[] chunkSize = ByteBuffer.allocate(4).putInt(read).array();
                        // send chunk length
                        out.write(chunkSize);
                        // send the chunk of data corresponding to chunk length
                        out.write(buffer, 0, read);

                        if (ins.available() > 0) {
                            byte[] reply = readAll(ins);
                            String replyResponse = new String(reply, StandardCharsets.UTF_8);
                            log.debug("Scan aborted. Server reply response {} ", replyResponse);
                            if (isSizeLimitException(replyResponse)) {
                                throw new SystemException(ClamAVService.class, "Scan aborted due to INSTREAM size limit exceeded. Check the value for ClamAV property 'StreamMaxLength'",
                                        "Scan aborted because data size limit exceeded");
                            }
                            throw new IOException("ClamAV scan aborted. Reply from server: " + replyResponse);
                        }
                        read = fileContent.read(buffer);
                    }

                    // send 0 to mark end of stream
                    out.write(new byte[]{0, 0, 0, 0});
                    out.flush();

                    String scanResult = new String(IOUtils.toByteArray(ins));
                    log.debug("scan result {}", scanResult);
                    return scanResult;
                }
            }
        } catch (IOException e) {
            log.error("Error while scanning ", e);
            throw new SystemException(ClamAVService.class, "Error during scanning", "Error during scanning");
        }
    }

    private byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read;
        do {
            read = is.read(buf);
            temp.write(buf, 0, read);
        } while ((read > 0) && (is.available() > 0));
        return temp.toByteArray();
    }

    public boolean isScannedFileSafe(String reply) {
        return reply.contains("stream: OK") &&
                !reply.contains("FOUND") &&
                !reply.contains("ERROR");
    }

    public boolean isSizeLimitException(String reply) {
        return reply.startsWith(INSTREAM_SIZE_LIMIT_RESPONSE);
    }
}
