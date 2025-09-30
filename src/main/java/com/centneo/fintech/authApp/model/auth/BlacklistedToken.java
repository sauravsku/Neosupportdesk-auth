package com.centneo.fintech.authApp.model.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Calendar;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 1000)
    private String token;

    @Column(nullable = false)
    private Instant blacklistedAt;

    @Column(nullable = false)
    private Instant expiry; // New field for expiry time

    public BlacklistedToken() {
    }

    public BlacklistedToken(String token, Instant blacklistedAt, Instant expiry) {
        this.token = token;
        this.blacklistedAt = blacklistedAt;
        this.expiry = expiry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(Instant blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public void setExpiry(Instant expiry) {
        this.expiry = expiry;
    }

    public Calendar getExpiryAsCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(java.util.Date.from(expiry));
        return calendar;
    }
}
