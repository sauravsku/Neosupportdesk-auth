package com.centneo.fintech.authApp;

import com.centneo.fintech.authApp.config.LDAPProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableConfigurationProperties(LDAPProperties.class)
public class CentNeoSupportDeskAuthSvcApplication {

	public static void main(String[] args) {

			SpringApplication.run(CentNeoSupportDeskAuthSvcApplication.class, args);
	}
}
