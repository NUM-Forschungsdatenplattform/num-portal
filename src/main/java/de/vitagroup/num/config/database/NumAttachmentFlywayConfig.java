package de.vitagroup.num.config.database;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@ConditionalOnBean(name = "numAttachmentDatasource")
@Configuration
public class NumAttachmentFlywayConfig {

    @Value("${spring.flyway.numportal-attachment.locations}")
    private String flywayLocations;

    private final DataSource dataSource;

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
