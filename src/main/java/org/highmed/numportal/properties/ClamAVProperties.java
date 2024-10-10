package org.highmed.numportal.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * NOTE # clamd.conf setup
 * # Close the connection when the data size limit is exceeded.
 * # The value should match your MTA's limit for a maximum attachment size.
 * # Default: 100M
 * #StreamMaxLength 25M
 *
 * <p>
 * TemporaryDirectory
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "clam-av")
public class ClamAVProperties {


  /**
   * TCPAddr from clamd.conf
   */
  private String host;

  /**
   * TCPSocket from clamd.conf
   */
  private int port;

  private int readTimeout;

  private int connectionTimeout;

}
