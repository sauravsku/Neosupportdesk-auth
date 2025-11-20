package com.centneo.fintech.authApp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPreferenceDto {

    private String ssoId;

    private String ssoName;

    private String emailId;

    private String journey;

    private String supportLevel;

    private Integer branchCode;
}
