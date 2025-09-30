package com.centneo.fintech.authApp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.ldap")
public class LDAPProperties {
    private String urls;
    private String base;
    private String username;
    private String password;
    private String userDnPatterns;
}
