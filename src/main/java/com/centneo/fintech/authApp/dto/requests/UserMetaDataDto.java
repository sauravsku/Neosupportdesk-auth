package com.centneo.fintech.authApp.dto.requests;

import lombok.Getter;
import lombok.Setter;

public record UserMetaDataDto(
        Long userId,
        String username,
        String supportLevel) {}
