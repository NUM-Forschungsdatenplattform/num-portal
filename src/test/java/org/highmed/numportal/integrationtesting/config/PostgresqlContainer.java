package org.highmed.numportal.integrationtesting.config;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresqlContainer extends PostgreSQLContainer<PostgresqlContainer> {
  private static final String IMAGE_VERSION = "postgres:12.14";
  private static PostgresqlContainer container;

  private PostgresqlContainer(String databaseName) {
    super(IMAGE_VERSION);
    this.withDatabaseName(databaseName);
  }

  public static PostgresqlContainer getInstance(String databaseName) {
    if (container == null) {
      container = new PostgresqlContainer(databaseName);
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
