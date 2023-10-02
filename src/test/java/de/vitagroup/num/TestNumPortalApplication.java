package de.vitagroup.num;

import de.vitagroup.num.config.database.NumAttachmentFlywayConfig;
import de.vitagroup.num.config.database.NumPortalAttachmentDatasourceConfiguration;
import de.vitagroup.num.listeners.UserCacheInit;
import de.vitagroup.num.service.atna.AtnaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties({AtnaProperties.class})
@EnableAsync
@ComponentScan(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        value = { UserCacheInit.class,
                NumPortalApplication.class,
                NumPortalAttachmentDatasourceConfiguration.class, NumAttachmentFlywayConfig.class}))
public class TestNumPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(TestNumPortalApplication.class, args);
  }
}
