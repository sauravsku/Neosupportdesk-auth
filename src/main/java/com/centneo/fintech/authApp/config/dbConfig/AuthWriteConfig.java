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
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = DataSourceConfigConstants.AUTH_WRITE_ENTITY_MANAGER_FACTORY,
        transactionManagerRef = DataSourceConfigConstants.AUTH_WRITE_TRANSACTION_MANAGER,
        basePackages = {DataSourceConfigConstants.AUTH_WRITE_BASE_PACKAGE})
public class AuthWriteConfig {


    /**
     * MasterDataSource Properties from yml.
     *
     * @return properties.
     */
    @Primary
    @Bean(name = DataSourceConfigConstants.AUTH_WRITE_DS_PROPERTIES)
    @ConfigurationProperties(prefix = "spring.datasource.write")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * DataSource for master node.
     *
     * @return datasource.
     */
    @Primary
    @Bean(name = DataSourceConfigConstants.AUTH_WRITE_DS)
    @ConfigurationProperties(prefix = "spring.datasource.write.hikari")
    public DataSource dataSource(@Qualifier(DataSourceConfigConstants.AUTH_WRITE_DS_PROPERTIES)
                                 DataSourceProperties masterDataSourceProperties) {
        return masterDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * Primary Write Entity Manager Factory.
     *
     * @param builder    builder for entity manger.
     * @param dataSource data source.
     * @return factory.
     */
    @Primary
    @Bean(name = DataSourceConfigConstants.AUTH_WRITE_ENTITY_MANAGER_FACTORY)
    public LocalContainerEntityManagerFactoryBean authWriteEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier(DataSourceConfigConstants.AUTH_WRITE_DS) DataSource dataSource) {
        return builder.dataSource(dataSource).packages(DataSourceConfigConstants.AUTH_MODEL_PACKAGE)
                .persistenceUnit("auth_write_pu").properties(DataSourceConfigConstants.ADDITIONAL_PROPERTIES).build();
    }


    @Primary
    @Bean(name = "authWriteTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier(DataSourceConfigConstants.AUTH_WRITE_ENTITY_MANAGER_FACTORY)
            EntityManagerFactory authWriteEntityManagerFactory) {
        return new JpaTransactionManager(authWriteEntityManagerFactory);
    }


    @Primary
    @Bean(name = "jdbcTemplateAuthWrite")
    public JdbcTemplate jdbcTemplateAuthWrite(
            @Qualifier(DataSourceConfigConstants.AUTH_WRITE_DS) DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Primary
    @Bean(name = "paramJdbcTemplateAuthWrite")
    public NamedParameterJdbcTemplate parameterJdbcTemplateAuthWrite(
            @Qualifier(DataSourceConfigConstants.AUTH_WRITE_DS) DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}