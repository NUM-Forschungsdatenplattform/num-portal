package de.vitagroup.num;

import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

/**
 * Use this {@link ApplicationContextInitializer} to setup a PostgreSQL instance started
 * in Docker.
 */
@Slf4j
public class PostgresInitializer implements
    ApplicationContextInitializer<ConfigurableApplicationContext> {

  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer(
      DockerImageName.parse("harbor.vitasystems.dev/dockerhub-proxy/library/postgres:12.4").asCompatibleSubstituteFor("postgres:12.4"));

  public static Map<String, String> getProperties() {
    Startables.deepStart(Stream.of(postgres)).join();

    log.info("POSTGRES_URL : {}", postgres.getJdbcUrl());
    log.info("POSTGRES_USERNAME: {}", postgres.getUsername());
    log.info("POSTGRES_PASSWORD: {}", postgres.getPassword());
    return Map.of(
        "spring.datasource.url", postgres.getJdbcUrl(),
        "spring.datasource.username", postgres.getUsername(),
        "spring.datasource.password", postgres.getPassword(),
        "spring.flyway.url", postgres.getJdbcUrl(),
        "spring.flyway.user", postgres.getUsername(),
        "spring.flyway.password", postgres.getPassword()
    );
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var env = applicationContext.getEnvironment();
    env.getPropertySources().addFirst(new MapPropertySource(
        "postgres-initializer",
        (Map) getProperties()
    ));
  }
}