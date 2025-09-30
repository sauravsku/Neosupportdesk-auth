package com.centneo.fintech.authApp.dto;

import java.util.List;

public record WUserDto(
        Long id,
        String username,
        String email,
        String entityNo,
        boolean isMfaRegistered,
        String supportLevel,
        List<String> roles
) {
}
