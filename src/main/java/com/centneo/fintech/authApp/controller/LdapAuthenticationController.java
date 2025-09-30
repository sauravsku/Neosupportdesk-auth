//package com.centneo.fintech.authApp.controller;
//
//import com.centneo.fintech.authApp.dto.EntityResponse;
//import com.centneo.fintech.authApp.dto.LoginRequestDto;
//import com.centneo.fintech.authApp.dto.UserRegisterRequestDTO;
//import com.centneo.fintech.authApp.service.WUserService;
//import com.centneo.fintech.authApp.util.JWTTokenUtil;
//import com.centneo.fintech.authApp.util.JwtUtil;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/auth")
//public class LdapAuthenticationController {
//
//    @Autowired
//    private AuthenticationManager authManager;
//
//    @Autowired
//    private JWTTokenUtil jwtTokenUtil;
//
//
//    @Autowired
//    WUserService userService;
//
//    @Autowired
//    PasswordEncoder passwordEncoder;
//
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
//        try {
//            Authentication auth = authManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
//            );
//            // Generate JWT token here if needed
//            String token = jwtUtil.generateToken(auth.getName(), auth.getAuthorities());
//            return ResponseEntity.ok(Map.of("token", token));
//        } catch (AuthenticationException e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
//        }
//    }
//
//    @PostMapping("register")
//    public ResponseEntity<Object> register(@RequestBody UserRegisterRequestDTO request){
//        request.setPassword(null);
//        return EntityResponse.generateResponse(null, "Register User", HttpStatus.OK, userService.createUser(request),
//                false, false);
//    }
//}
