package de.vitagroup.num.config.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(basePackages = {"de.vitagroup.num.domain", "de.vitagroup.num.service"},
        entityManagerFactoryRef = "numEntityManagerFactory",
        transactionManagerRef = "numTransactionManager")
@EnableTransactionManagement
public class NumPortalDatasourceConfiguration {

    @Value("${spring.jpa.show-sql}")
    private boolean showSql;

    @Primary
    @Bean(name = "numPortalProperties")
    @ConfigurationProperties(prefix = "spring.datasource.numportal")
    public DataSourceProperties dataSourceProperties(){
        return new DataSourceProperties();
    }

    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari.data-source-properties")
    @Bean(name = "hikariProps")
    public Properties hikaryProperties() {
      return new Properties();
    }

    @Primary
    @Bean("numPortalDatasource")
    @FlywayDataSource
    public DataSource dataSource(@Qualifier("numPortalProperties") DataSourceProperties dataSourceProperties, @Qualifier("hikariProps") Properties hikaryProps) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(dataSourceProperties().getDriverClassName());
        hikariConfig.setJdbcUrl(dataSourceProperties.getUrl());
        hikariConfig.setUsername(dataSourceProperties.getUsername());
        hikariConfig.setPassword(dataSourceProperties.getPassword());
        hikariConfig.getDataSourceProperties().putAll(hikaryProps);
        DataSource numDataSource = new HikariDataSource(hikariConfig);
        return numDataSource;
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean numEntityManagerFactory(ConfigurableListableBeanFactory beanFactory,
                                                                          @Qualifier("numPortalDatasource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setDataSource(dataSource);
        localContainerEntityManagerFactoryBean.setPackagesToScan("de.vitagroup.num.domain", "de.vitagroup.num.service");
        localContainerEntityManagerFactoryBean.setPersistenceUnitName("numPortal");
        localContainerEntityManagerFactoryBean.getJpaPropertyMap().put(AvailableSettings.BEAN_CONTAINER, new SpringBeanContainer(beanFactory));
        localContainerEntityManagerFactoryBean.getJpaPropertyMap().put(AvailableSettings.IMPLICIT_NAMING_STRATEGY, new SpringImplicitNamingStrategy());
        localContainerEntityManagerFactoryBean.getJpaPropertyMap().put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, new CamelCaseToUnderscoresNamingStrategy());
        localContainerEntityManagerFactoryBean.getJpaPropertyMap().put("hibernate.order_by.default_null_ordering", "last");
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setGenerateDdl(false);
        hibernateJpaVendorAdapter.setShowSql(showSql);

        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(hibernateJpaVendorAdapter);
        return localContainerEntityManagerFactoryBean;
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.jpa")
    public PlatformTransactionManager numTransactionManager(
            @Qualifier("numEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactoryBean.getObject()));
    }

}
