package org.highmed.numportal.config.database;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;

@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
@Configuration
@EnableJpaRepositories(basePackages = "org.highmed.numportal.attachment",
        entityManagerFactoryRef = "attachmentEntityManagerFactory",
        transactionManagerRef = "attachmentTransactionManager")
@EnableTransactionManagement
public class NumPortalAttachmentDatasourceConfiguration {

    @Value("${spring.jpa.show-sql}")
    private boolean showSql;


    @Bean(name = "numAttachmentProperties")
    @ConfigurationProperties(prefix = "spring.datasource.numportal-attachment")
    public DataSourceProperties dataSourceProperties(){
        return new DataSourceProperties();
    }

    @Bean("numAttachmentDatasource")
    public DataSource dataSource(@Qualifier("numAttachmentProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean attachmentEntityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                                 @Qualifier("numAttachmentDatasource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = builder
                .dataSource(dataSource)
                .packages("org.highmed.numportal.attachment")
                .persistenceUnit("numAttachment")
                .build();
        localContainerEntityManagerFactoryBean.getJpaPropertyMap().put(AvailableSettings.IMPLICIT_NAMING_STRATEGY, new SpringImplicitNamingStrategy());
        localContainerEntityManagerFactoryBean.getJpaPropertyMap().put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, new CamelCaseToUnderscoresNamingStrategy());
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setGenerateDdl(false);
        hibernateJpaVendorAdapter.setShowSql(showSql);

        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(hibernateJpaVendorAdapter);
        return localContainerEntityManagerFactoryBean;
    }

    @Bean
    public PlatformTransactionManager attachmentTransactionManager(
            @Qualifier("attachmentEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactoryBean.getObject()));
    }
}
