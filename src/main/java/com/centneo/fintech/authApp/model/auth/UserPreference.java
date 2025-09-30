package com.centneo.fintech.authApp.model.auth;

import com.centneo.fintech.authApp.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "user_preference")
public class UserPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "user_journey")
    private String userJourney;

    @Column(name = "preferences")
    private String preferences;
}
