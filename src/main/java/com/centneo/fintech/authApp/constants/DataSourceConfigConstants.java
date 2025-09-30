package com.centneo.fintech.authApp.constants;

import java.util.HashMap;
import java.util.Map;

public class DataSourceConfigConstants {

    public static final String PRIMARY_WRITE_ENTITY_MANAGER_FACTORY = "PrimaryWriteEntityManagerFactory";

    public static final String PRIMARY_READ_ENTITY_MANAGER_FACTORY = "PrimaryReadEntityManagerFactory";

    public static final String AUTH_READ_ENTITY_MANAGER_FACTORY = "authReadEntityManagerFactory";

    public static final String AUTH_WRITE_ENTITY_MANAGER_FACTORY = "authWriteEntityManagerFactory";

    public static final String PRIMARY_READ_TRANSACTION_MANAGER = "primaryReadTransactionManager";

    public static final String PRIMARY_WRITE_TRANSACTION_MANAGER = "primaryWriteTransactionManager";

    public static final String AUTH_READ_TRANSACTION_MANAGER = "authReadTransactionManager";

    public static final String AUTH_WRITE_TRANSACTION_MANAGER = "authWriteTransactionManager";

    public static final String PRIMARY_WRITE_DS_PROPERTIES = "primaryWriteDsProperties";

    public static final String PRIMARY_READ_DS_PROPERTIES = "primaryReadDsProperties";

    public static final String AUTH_WRITE_DS_PROPERTIES = "authWriteDsProperties";

    public static final String AUTH_READ_DS_PROPERTIES = "authReadDsProperties";

    public static final Map<String, String> ADDITIONAL_PROPERTIES = additionalJpaProperties();

    public static final String AUTH_WRITE_DS = "authWriteDataSource";

    public static final String AUTH_READ_DS = "authReadDataSource";

    public static final String PRIMARY_WRITE_DS = "primaryWriteDataSource";

    public static final String PRIMARY_READ_DS = "primaryReadDataSource";

    public static final String AUTH_DS = "authDataSource";

    public static final String PRIMARY_MODEL_PACKAGE = "com.centneo.fintech.authApp.model.primary";

    public static final String PRIMARY_REPO_BASE_PACKAGE = "com.centneo.fintech.authApp.repository.primary";

    public static final String AUTH_REPO_BASE_PACKAGE = "com.centneo.fintech.authApp.repository.auth";



    public static final String PRIMARY_WRITE_BASE_PACKAGE = "com.centneo.fintech.authApp.repository.primary.write";

    public static final String PRIMARY_READ_BASE_PACKAGE = "com.centneo.fintech.authApp.repository.primary.read";

    public static final String AUTH_WRITE_BASE_PACKAGE = "com.centneo.fintech.authApp.repository.write";

    public static final String AUTH_READ_BASE_PACKAGE = "com.centneo.fintech.authApp.repository.read";



    public static final String AUTH_READ_WRITE_BASE_PACKAGE = "com.centneo.fintech.authApp.repository.primary.read";

    public static final String AUTH_MODEL_PACKAGE = "com.centneo.fintech.authApp.model.auth";

    private static Map<String, String> additionalJpaProperties() {
        Map<String, String> map = new HashMap<>();
        map.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
        return map;
    }
}

