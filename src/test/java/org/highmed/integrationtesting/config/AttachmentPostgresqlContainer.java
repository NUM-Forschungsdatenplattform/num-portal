package org.highmed.integrationtesting.config;

import org.testcontainers.containers.PostgreSQLContainer;

public class AttachmentPostgresqlContainer extends PostgreSQLContainer<AttachmentPostgresqlContainer> {
  private static final String IMAGE_VERSION = "postgres:12.14";
  private static AttachmentPostgresqlContainer container;

  private AttachmentPostgresqlContainer(String databaseName) {
    super(IMAGE_VERSION);
    this.withDatabaseName(databaseName);
  }

  public static AttachmentPostgresqlContainer getInstance(String databaseName) {
    if (container == null) {
      container = new AttachmentPostgresqlContainer(databaseName);
    }
    return container;
  }

  @Override
  public void start() {
    super.start();
    System.setProperty("ATTACHMENT_DB_URL", container.getJdbcUrl());
    System.setProperty("ATTACHMENT_DB_USERNAME", container.getUsername());
    System.setProperty("ATTACHMENT_DB_PASSWORD", container.getPassword());
  }

  @Override
  public void stop() {
    // do nothing, JVM handles shut down
  }
}
