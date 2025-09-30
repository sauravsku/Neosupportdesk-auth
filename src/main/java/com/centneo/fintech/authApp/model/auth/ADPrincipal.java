package com.centneo.fintech.authApp.model.auth;


import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.naming.*;
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entry(objectClasses = { "inetOrgPerson", "top", "person"})
public final class ADPrincipal {


    @Id
    private Name id;

    @Attribute(name="cn")
    private String cn;

    @Attribute(name="password")
    String password;

    @Attribute(name="sn")
    String sn;

    @Attribute(name = "uid")
    private String uid;

}
