package de.vitagroup.num.integrationtesting.config;

import org.testcontainers.containers.PostgreSQLContainer;

public class NumPostgresqlContainer extends PostgreSQLContainer<NumPostgresqlContainer> {
  private static final String IMAGE_VERSION = "postgres:12.14";
  private static NumPostgresqlContainer container;

  private NumPostgresqlContainer(String databaseName) {
    super(IMAGE_VERSION);
    this.withDatabaseName(databaseName);
  }

  public static NumPostgresqlContainer getInstance(String databaseName) {
    if (container == null) {
      container = new NumPostgresqlContainer(databaseName);
    }
    return container;
  }

  @Override
  public void start() {
    super.start();
    System.setProperty("DB_URL", container.getJdbcUrl());
    System.setProperty("DB_USERNAME", container.getUsername());
    System.setProperty("DB_PASSWORD", container.getPassword());
  }

  @Override
  public void stop() {
    // do nothing, JVM handles shut down
  }
}
