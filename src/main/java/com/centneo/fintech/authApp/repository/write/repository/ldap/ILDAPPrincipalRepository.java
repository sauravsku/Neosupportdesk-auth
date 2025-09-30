package com.centneo.fintech.authApp.repository.write.repository.ldap;

import com.centneo.fintech.authApp.model.auth.ADPrincipal;
import org.springframework.data.ldap.repository.LdapRepository;

public interface ILDAPPrincipalRepository extends LdapRepository<ADPrincipal>{
	ADPrincipal findByCn(String cn);
	ADPrincipal findByCnAndPassword(String cn, String password);

	ADPrincipal findByUid(String username);
}