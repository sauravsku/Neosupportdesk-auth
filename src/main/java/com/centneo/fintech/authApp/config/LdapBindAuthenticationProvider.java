package com.centneo.fintech.authApp.config;


import com.centneo.fintech.authApp.model.auth.ADPrincipal;
import com.centneo.fintech.authApp.repository.write.repository.ldap.ILDAPPrincipalRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.ldap.core.support.LdapContextSource;

import java.util.List;

@Component
public class LdapBindAuthenticationProvider implements AuthenticationProvider {

    private final ILDAPPrincipalRepository ldaPrincipalRepository;

    public LdapBindAuthenticationProvider(ILDAPPrincipalRepository ldaPrincipalRepository) {
        this.ldaPrincipalRepository = ldaPrincipalRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        ADPrincipal principal = ldaPrincipalRepository.findByUid(username);
        if (principal == null) {
            throw new BadCredentialsException("Invalid username");
        }

        String userDn = "uid=" + username + ",dc=example,dc=com"; // Adjust to your LDAP base DN

        try {
            LdapContextSource contextSource = new LdapContextSource();
            contextSource.setUrl("ldap://ldap.forumsys.com:389");
            contextSource.setUserDn(userDn);
            contextSource.setPassword(password);
            contextSource.afterPropertiesSet();
            contextSource.getReadOnlyContext().close();
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid password", e);
        }

        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

