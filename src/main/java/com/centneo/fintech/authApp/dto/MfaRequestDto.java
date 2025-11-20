package com.centneo.fintech.authApp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaRequestDto {

    private String ssoId;

    private String mfaCode;
}
