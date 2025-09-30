package com.centneo.fintech.authApp.service;

import com.centneo.fintech.authApp.model.auth.BlacklistedToken;
import com.centneo.fintech.authApp.repository.write.repository.BlacklistTokenRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class LogoutService {

    private final BlacklistTokenRepository blacklistRepo;

    @Value("${jwt.secret:jkashdjkahsjkdh}")
    private String jwtSecret;

    public LogoutService(BlacklistTokenRepository blacklistRepo) {
        this.blacklistRepo = blacklistRepo;
    }

    public void logout(String token) {
        try {
            Date expiry = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();

            BlacklistedToken blacklistedToken = new BlacklistedToken(
                    token,
                    Instant.now(),
                    expiry.toInstant()
            );
            blacklistRepo.save(blacklistedToken);
        } catch (JwtException e) {
            // Handle invalid token
        }
    }

    public boolean isBlacklisted(String token) {
        return blacklistRepo.isBlacklisted(token);
    }
}
