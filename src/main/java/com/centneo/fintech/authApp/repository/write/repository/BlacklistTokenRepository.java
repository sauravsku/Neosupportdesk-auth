package com.centneo.fintech.authApp.repository.write.repository;

import com.centneo.fintech.authApp.model.auth.BlacklistedToken;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
@Repository
public class BlacklistTokenRepository {

    private final StringRedisTemplate redisTemplate;

    public BlacklistTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(BlacklistedToken token) {
        Instant now = Instant.now();
        long ttl = Duration.between(now, token.getExpiry()).getSeconds();

        if (ttl > 0) {
            redisTemplate.opsForValue().set(token.getToken(), "blacklisted", ttl, TimeUnit.SECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }

    // Remove findByToken or leave unimplemented as Redis doesn't store full object
    public Optional<BlacklistedToken> findByToken(String token) {
        // Not implemented, use isBlacklisted instead
        return Optional.empty();
    }
}
