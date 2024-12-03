package org.highmed.numportal.config.database;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
@Configuration
public class NumAttachmentFlywayConfig {

  private final DataSource dataSource;
  @Value("${spring.flyway.numportal-attachment.locations}")
  private String flywayLocations;

  public NumAttachmentFlywayConfig(@Qualifier("numAttachmentDatasource") DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostConstruct
  public void executeMigration() {
    Flyway.configure()
          .dataSource(dataSource)
          .locations(flywayLocations)
          .baselineOnMigrate(true)
          .load()
          .migrate();
  }
}
