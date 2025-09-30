package com.centneo.fintech.authApp.config.dbConfig;

import com.centneo.fintech.authApp.constants.DataSourceConfigConstants;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = DataSourceConfigConstants.AUTH_READ_ENTITY_MANAGER_FACTORY,
        transactionManagerRef = DataSourceConfigConstants.AUTH_READ_TRANSACTION_MANAGER,
        basePackages = {DataSourceConfigConstants.AUTH_READ_BASE_PACKAGE})
public class AuthReadConfig {

    @Bean(name = "AuthReadDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.read")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "AuthReadDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.read.hikari")
    public DataSource dataSource(@Qualifier("AuthReadDataSourceProperties")
                                 DataSourceProperties authReadDataSourceProperties) {
        return authReadDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = DataSourceConfigConstants.AUTH_READ_ENTITY_MANAGER_FACTORY)
    public LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean(
            EntityManagerFactoryBuilder builder,
            @Qualifier("AuthReadDataSource") DataSource dataSource) {
        return builder.dataSource(dataSource).packages(DataSourceConfigConstants.AUTH_MODEL_PACKAGE).
                persistenceUnit("auth_read_pu").properties(DataSourceConfigConstants.ADDITIONAL_PROPERTIES).build();
    }

    @Bean(name = "authReadTransactionManager")
    public PlatformTransactionManager platformTransactionManager(
            @Qualifier("authReadEntityManagerFactory") EntityManagerFactory authReadEntityManagerFactory) {
        return new JpaTransactionManager(authReadEntityManagerFactory);
    }

    @Bean(name = "jdbcTemplateAuthRead")
    public JdbcTemplate jdbcTemplateAuthRead(
            @Qualifier("AuthReadDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}