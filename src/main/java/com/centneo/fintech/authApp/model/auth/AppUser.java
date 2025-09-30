package com.centneo.fintech.authApp.model.auth;

import com.centneo.fintech.authApp.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Builder
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    @Id
    @Column(name = "user_id", insertable = true, updatable = false)
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "email_id")
    private String emailId;

    @Column(name = "user_phone")
    private String phoneNo;

    @Column(name = "ip_phone_alloc")
    private String ipPhoneAlloc;

    @Column(name = "curr_ip")
    private String currIp;

    @Column(name = "user_level")
    private String userLevel;

    @Column(name = "user_role")
    private String userRole;

    @Column(name = "last_Active")
    private Timestamp lastActive;

}
