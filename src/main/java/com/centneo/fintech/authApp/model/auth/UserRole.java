package com.centneo.fintech.authApp.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "user_role")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRole implements Serializable {

    private static final long serialVersionUID = 5926468583005150707L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}


//User and Role have bidirectional @OneToMany with UserRole as the join entity.
//
//UserRole has two @ManyToOne fields referencing User and Role.
//
//Using FetchType.EAGER on userRoles in User because Spring Security usually requires roles immediately, but you can adjust based on your needs.
//
//        UserDetails interface methods are implemented in User with defaults; customize your business logic as needed.
//
//Avoid persisting password if using LDAP or external authentication, so @Transient is used on password.
//
//Adjust your Oracle DB schema accordingly with tables: WUser, Role, UserRole.
//
//Make sure you add the required tables with correct data types for IDs and foreign keys.