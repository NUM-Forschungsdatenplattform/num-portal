package de.vitagroup.num.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "clam-av")
public class ClamAVProperties {

    /**
     * NOTE # clamd.conf setup
     * # Close the connection when the data size limit is exceeded.
     * # The value should match your MTA's limit for a maximum attachment size.
     * # Default: 100M
     * #StreamMaxLength 25M
     *
     * TemporaryDirectory
     */

    /**
     * TCPAddr from clamd.conf
     */
    private String host;

    /**
     * TCPSocket from clamd.conf
     */
    private int port;

    private int timeout;

    private int pingTimeout;

}
