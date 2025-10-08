package com.centneo.fintech.authApp.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class MfaService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public GoogleAuthenticatorKey generateSecretKey() {
        return gAuth.createCredentials();
    }

    public boolean verifyCode(String secret, int code) {
        GoogleAuthenticator authenticator = new GoogleAuthenticator();
        return gAuth.authorize(secret, code);
    }

    public String getQrCodeImageUrl(String username, String secret) {
        String issuer = "CentNeoSupportDesk"; // Your app name
        String otpAuthUrl = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                URLEncoder.encode(issuer, StandardCharsets.UTF_8),
                URLEncoder.encode(username, StandardCharsets.UTF_8),
                secret,
                URLEncoder.encode(issuer, StandardCharsets.UTF_8)
        );

        // Wrap it inside Google Chart API to generate a QR code
        return "https://chart.googleapis.com/chart?chs=200x200&cht=qr&chl="
                + URLEncoder.encode(otpAuthUrl, StandardCharsets.UTF_8);
    }
}

