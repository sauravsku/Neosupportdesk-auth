package com.centneo.fintech.authApp.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "WUser")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements Serializable, UserDetails {

    private static final long serialVersionUID = 5926468583005150707L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Transient // Do NOT persist password if you don't want
    private String password;

    @Column(name = "entity_no", unique = true)
    private String entityNo;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "isMfaRegistered")
    private boolean isMfaRegistered;

    @Column(name = "mfaSecret")
    private String mfaSecret;

    @Column(name = "supportLevel")
    private String supportLevel;

    @Column(name = "branchCode")
    private Integer branchCode;

    // One user can have many userRoles (join table)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<UserRole> userRoles = new ArrayList<>();

    // Convenience getter for roles as strings
    public List<String> getRoles() {
        return userRoles.stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());
    }

    // Spring Security authorities from roles
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userRoles.stream()
                .map(userRole -> (GrantedAuthority) () -> userRole.getRole().getName())
                .collect(Collectors.toList());
    }

    // UserDetails interface methods
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Customize as needed
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Customize as needed
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Customize as needed
    }

    @Override
    public boolean isEnabled() {
        return true; // Customize as needed
    }
}
