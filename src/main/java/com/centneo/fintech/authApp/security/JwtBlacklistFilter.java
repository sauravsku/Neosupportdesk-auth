package com.centneo.fintech.authApp.security;

import com.centneo.fintech.authApp.repository.write.repository.BlacklistTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component

public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final BlacklistTokenRepository blacklistRepo;

    @Autowired
    JWTTokenUtil jwtTokenUtil;

    public JwtBlacklistFilter(BlacklistTokenRepository blacklistRepo, JWTTokenUtil jwtTokenUtil) {
        this.blacklistRepo = blacklistRepo;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = jwtTokenUtil.extractToken(request);
        if (token != null && blacklistRepo.isBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

