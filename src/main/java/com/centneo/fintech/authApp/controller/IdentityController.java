package com.centneo.fintech.authApp.controller;

import com.centneo.fintech.authApp.dto.*;
import com.centneo.fintech.authApp.enums.SupportLevelEnum;
import com.centneo.fintech.authApp.model.auth.User;
import com.centneo.fintech.authApp.repository.read.repository.IUserRepositoryReadOnly;
import com.centneo.fintech.authApp.repository.write.repository.IUserRepository;
import com.centneo.fintech.authApp.repository.write.repository.ldap.ILDAPPrincipalRepository;
import com.centneo.fintech.authApp.security.JWTTokenUtil;
import com.centneo.fintech.authApp.service.LogoutService;
import com.centneo.fintech.authApp.service.MfaService;
import com.centneo.fintech.authApp.service.WUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("identity")
public class IdentityController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IUserRepositoryReadOnly userRepositoryReadOnly;

    @Autowired
    IUserRepository userRepository;

    @Autowired
    WUserService userService;

    @Autowired
    MfaService mfaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    LogoutService logoutService;

    @Autowired
    private JWTTokenUtil jwtTokenUtil;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private ILDAPPrincipalRepository ldaPrincipalRepository;


    // ====== LOGIN ======
    @PostMapping("/token")
    public ResponseEntity<Object> createAuthenticationToken(
            @RequestBody AuthenticationRequest authenticationRequest,
            HttpServletResponse response) {

        // 1️⃣ LDAP Authentication
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getSsoId(),
                            authenticationRequest.getPassword()
                    )
            );
        } catch (Exception e) {
            return EntityResponse.generateResponse(
                    authenticationRequest.getSsoId(),
                    "Authentication",
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials, please check details and try again.",
                    true,
                    false
            );
        }

        // 2️⃣ Check if user exists in DB, else create
        User user = userRepositoryReadOnly.findBySsoId(authenticationRequest.getSsoId());
        if (user == null) {
            UserRegisterRequestDTO userRegisterRequestDTO = new UserRegisterRequestDTO();
            userRegisterRequestDTO.setEntityNo(UUID.randomUUID().toString());
            userRegisterRequestDTO.setSsoId(authenticationRequest.getSsoId());
            userRegisterRequestDTO.setRoleList(List.of("USER"));
            user = userService.createUser(userRegisterRequestDTO);
        }

        // 3️⃣ JWT Generation
        final UserDetails userDetails = userService.loadUserByUsername(user.getSsoId());
        final String token = jwtTokenUtil.generateAccessToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        // 4️⃣ Set Cookies (HttpOnly)
        Cookie jwtCookie = new Cookie("jwt_token", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Set to true in production (HTTPS)
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(60 * 60); // 1 minute

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Set true in production
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(24 * 60 * 60); // 1 day

        response.addCookie(jwtCookie);
        response.addCookie(refreshCookie);

        // 5️⃣ Return MFA flags & tokens to frontend

        boolean isMfaRegistered = user.isMfaRegistered();
        boolean mfaRequired = isMfaRegistered;

        return ResponseEntity.ok(
                EntityResponse.generateResponse(
                        authenticationRequest.getSsoId(),
                        "Authentication successful",
                        HttpStatus.OK,
                        new AuthenticationResponse(token, refreshToken),
                        mfaRequired,
                        isMfaRegistered
                )
        );
    }


    // ====== REFRESH TOKEN ======
    @PostMapping("/refresh-token")
    public ResponseEntity<Object> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null || !jwtTokenUtil.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(EntityResponse.generateResponse(null,"Refresh Token",
                            HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired",
                            false, false));
        }

        String username = jwtTokenUtil.getUsernameFromRefreshToken(refreshToken);
        UserDetails userDetails = userService.loadUserByUsername(username);

        String newJwt = jwtTokenUtil.generateAccessToken(userDetails);
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        Cookie jwtCookie = new Cookie("jwt_token", newJwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(60*60); // 1 minute

        Cookie refreshCookie = new Cookie("refresh_token", newRefreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(24 * 60 * 60); // 1 day

        response.addCookie(jwtCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(EntityResponse.generateResponse(username, "Refresh Token", HttpStatus.OK,
                new AuthenticationResponse(newJwt, newRefreshToken), false, false));
    }


    // ====== MFA REGISTER ======
    @PostMapping("/mfa/register")
    public ResponseEntity<?> registerMfa(@RequestParam String ssoId) {
        User user = userRepositoryReadOnly.findBySsoId(ssoId);

        if (user.isMfaRegistered()) {
            return ResponseEntity.ok().body(Map.of(
                    "isMfaRegistered", user.isMfaRegistered(),
                    "Message", "MFA already registered"
            ));
        }

        GoogleAuthenticatorKey key = mfaService.generateSecretKey();
        String secret = key.getKey();

        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrCodeUrl = mfaService.getQrCodeImageUrl(ssoId, secret);

        return ResponseEntity.ok(Map.of(
                "qrCodeUrl", qrCodeUrl,
                "secret", secret
        ));
    }


    // ====== MFA VERIFY ======
    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaRequestDto mfaRequestDto) {
        User user = userRepositoryReadOnly.findBySsoId(mfaRequestDto.getSsoId());

        boolean isCodeValid = mfaService.verifyCode(user.getMfaSecret(), Integer.parseInt(mfaRequestDto.getMfaCode()));

        if (!isCodeValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid MFA code"));
        }

        if (!user.isMfaRegistered()) {
            user.setMfaRegistered(true);
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("message", "MFA verified successfully"));
    }


    // ====== REGISTER USER (without password storage) ======
    @PostMapping("register")
    public ResponseEntity<Object> register(@RequestBody UserRegisterRequestDTO request){
        request.setPassword(null);
        return EntityResponse.generateResponse(request.getSsoId(), "Register User", HttpStatus.OK, userService.createUser(request),
                false, false);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("profile")
    public ResponseEntity<Object> retrieveUserProfile() {
        return EntityResponse.generateResponse(null, "User Profile", HttpStatus.OK, userService.findCurrentUser(), false, true);
    }


    // ====== LOGOUT ======
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtTokenUtil.extractToken(request);
        System.out.println("Logout token: " + token);  // Debug

        if (token != null && !token.isEmpty()) {
            logoutService.logout(token);  // Save token in blacklist
        } else {
            System.out.println("No token found in request");
        }

        // Clear JWT cookie
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        // Clear Refresh token cookie
        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok("Logged out successfully");
    }


    // ====== AUTH CHECK - Used by frontend to verify if user is authenticated ======
    @GetMapping("authCheck")
    @Transactional
    public ResponseEntity<Object> checkAuth(HttpServletRequest request) {
        String token = jwtTokenUtil.extractToken(request);
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(EntityResponse.generateResponse(
                            null,
                            null,
                            HttpStatus.UNAUTHORIZED,
                            "No token provided",
                            false,
                            false
                    ));
        }
        UserDetails userDetails;
        try {
            String username = jwtTokenUtil.getUsernameFromToken(token);
            userDetails = userService.loadUserByUsername(username);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(EntityResponse.generateResponse(
                            null,
                            null,
                            HttpStatus.UNAUTHORIZED,
                            "Invalid token",
                            false,
                            false
                    ));
        }

        if (!jwtTokenUtil.validateToken(token, userDetails)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(EntityResponse.generateResponse(
                            userDetails.getUsername(),
                            null,
                            HttpStatus.UNAUTHORIZED,
                            "Invalid or expired token",
                            false,
                            false
                    ));
        }

        if (logoutService.isBlacklisted(token)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(EntityResponse.generateResponse(
                            userDetails.getUsername(),
                            null,
                            HttpStatus.UNAUTHORIZED,
                            "Token blacklisted",
                            false,
                            false
                    ));
        }

        User user = userRepositoryReadOnly.findBySsoId(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(EntityResponse.generateResponse(
                            null,
                            null,
                            HttpStatus.UNAUTHORIZED,
                            "User not found",
                            false,
                            false
                    ));
        }

        // Prepare real JSON object
        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getSsoId());
        data.put("email", user.getEmail());
        data.put("roles", user.getRoles());
        data.put("supportLevel", user.getSupportLevel());
        data.put("branchCode", user.getBranchCode());

        return ResponseEntity.ok(
                EntityResponse.generateResponse(
                        user.getSsoId(),
                        "User authenticated",
                        HttpStatus.OK,
                        data,
                        !user.isMfaRegistered(),
                        user.isMfaRegistered()
                )
        );
    }

    @PostMapping("/user/preferences")
    public ResponseEntity<Object> setUserPreferences(HttpServletRequest request,
                                                     @RequestBody UserPreferenceDto userPreferenceDto) {

        ResponseEntity<Object> authResponse = checkAuth(request);

        if (authResponse.getStatusCode() != HttpStatus.OK) {
            return EntityResponse.generateResponse(
                    userPreferenceDto.getSsoId(),
                    "User not authenticated",
                    HttpStatus.UNAUTHORIZED,
                    null,
                    false,
                    false
            );
        }

        // Convert authResponse body to a Map
        var authMap = objectMapper.convertValue(authResponse.getBody(), Map.class);

        // Extract "data"
        var userDataObj = authMap.get("body");
        if (!(userDataObj instanceof Map)) {
            return EntityResponse.generateResponse(
                    userPreferenceDto.getSsoId(),
                    "Invalid user data",
                    HttpStatus.BAD_REQUEST,
                    null,
                    false,
                    false
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) userDataObj;
        String ssoId = (String) userData.get("username");

        if (ssoId == null || ssoId.isBlank()) {
            return EntityResponse.generateResponse(
                    userPreferenceDto.getSsoId(),
                    "Invalid user data",
                    HttpStatus.BAD_REQUEST,
                    null,
                    false,
                    false
            );
        }

        try {
            User user = userService.setPreferences(ssoId, userPreferenceDto);
            return EntityResponse.generateResponse(
                    user.getSsoId(),
                    "User preferences saved successfully",
                    HttpStatus.OK,
                    user,
                    !user.isMfaRegistered(),
                    user.isMfaRegistered()
            );
        } catch (Exception e) {
            return EntityResponse.generateResponse(
                    ssoId,
                    "Failed to save preferences: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    null,
                    false,
                    true
            );
        }
    }

    @GetMapping("/getUserJourney")
    public ResponseEntity<ResponseDto<List<Long>>> getUserJourney(@RequestParam("ssoId") String username) {
        try {
            // Delegate to service which returns ResponseEntity<ResponseDto<List<Long>>>
            return userService.getUserJourneyId(username);
        } catch (Exception e) {
            // Return proper 500 response with correct generic type
            return ResponseEntity.status(500)
                    .body(ResponseDto.<List<Long>>builder()
                            .success(false)
                            .message("Error fetching user journey: " + e.getMessage())
                            .data(null)
                            .statusCode(500)
                            .build());
        }
    }

    @GetMapping("/getUserSearchInfo")
    public ResponseEntity<ResponseDto> getUserSearchInfo(
            @RequestParam("query") String searchUser,
            @RequestParam("supportLevel") String supportLevel) {

        try {
            // Normalize support level
            String userLevelInput = getRefactoredUserLevel(supportLevel);
            List<User> userList = userRepositoryReadOnly.findAllBySupportLevel(userLevelInput);

            if (!searchUser.isEmpty()) {
                // safe, preserves insertion order, merges duplicates by keeping the first value
                Map<String, String> searchRes = userList.stream()
                        .filter(u -> u != null && u.getSsoId() != null && searchUser != null)
                        .map(u -> new AbstractMap.SimpleEntry<>(
                                u.getSsoId().trim(),
                                u.getSupportLevel() == null
                                        ? ""
                                        : SupportLevelEnum.fromLabel(u.getSupportLevel()).name()  // convert to enum name
                        ))
                        .filter(e -> e.getKey().toLowerCase().contains(searchUser.trim().toLowerCase()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (existing, replacement) -> existing,          // merge duplicates: keep first
                                LinkedHashMap::new                            // preserve order
                        ));

                ResponseDto response = new ResponseDto(
                        true,
                        "User(s) fetched successfully",
                        searchRes,
                        200
                );
                return ResponseEntity.ok(response);
            }

            Map<String, String> getAllUsers = userList.stream()
                    .collect(Collectors.toMap(
                            User::getSsoId,                           // key = username
                            user -> SupportLevelEnum.fromLabel(user.getSupportLevel()).name(), // value
                            (existing, replacement) -> existing,         // on duplicate key, keep existing
                            LinkedHashMap::new                           // preserve order
                    ));
            ResponseDto response = new ResponseDto(
                        true,
                        "User(s) fetched successfully",
                        getAllUsers,
                        200
                );
                return ResponseEntity.ok(response);

        } catch (Exception e) {
            ResponseDto response = new ResponseDto(
                    false,
                    "Failed to fetch user: " + e.getMessage(),
                    null,
                    500
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    private String getRefactoredUserLevel(String supportLevel) {

        if (supportLevel.equalsIgnoreCase("L1"))
            return "Level-1";
        else if (supportLevel.equalsIgnoreCase("L2"))
            return "Level-2";
        else if (supportLevel.equalsIgnoreCase("L3"))
            return "Level-3";
        else
            return "Invalid Support Level";
    }

    @GetMapping("getAllActiveSupportUsers")
    public ResponseEntity<ResponseDto> getAllActiveSupportUsers() {

        try {
            return userService.getAllActiveSupportUsers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("getSsoName")
    public ResponseEntity<ResponseDto> getSsoName(@RequestParam("ssoId") String ssoId) {

        try {
            return userService.getSsoNameById(ssoId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
