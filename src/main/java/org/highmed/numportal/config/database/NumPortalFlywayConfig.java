package org.highmed.numportal.config.database;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
public class NumPortalFlywayConfig {

  private final DataSource dataSource;
  @Value("${spring.flyway.numportal.locations}")
  private String flywayLocations;

  public NumPortalFlywayConfig(@Qualifier("numPortalDatasource") DataSource dataSource) {
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
