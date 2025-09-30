package com.centneo.fintech.authApp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
public class LdapConfig {

    @Value("${spring.ldap.urls}")
    private String ldapUrls;

    @Value("${spring.ldap.base:}")
    private String ldapBase;

    @Value("${spring.ldap.username:}")
    private String managerDn;

    @Value("${spring.ldap.password:}")
    private String managerPassword;

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource ctx = new LdapContextSource();
        ctx.setUrl(ldapUrls);
        if (ldapBase != null && !ldapBase.isBlank()) {
            ctx.setBase(ldapBase);
        }
        if (managerDn != null && !managerDn.isBlank()) {
            ctx.setUserDn(managerDn);
            ctx.setPassword(managerPassword);
        }
        // important: initialise after properties set
        ctx.afterPropertiesSet();
        return ctx;
    }

    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }
}
