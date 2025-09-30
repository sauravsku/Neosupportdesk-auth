package com.centneo.fintech.authApp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Robust JWT utility supporting HS256 or RS256.
 *
 * Configuration examples (application.yml or env):
 *
 * jwt:
 *   alg: RS256               # or HS256
 *   issuer: centneo
 *   access-ttl-ms: 18000000
 *   refresh-ttl-ms: 86400000
 *   # For RS256:
 *   private-key-file: /etc/keys/jwt-private.pem
 *   public-key-file: /etc/keys/jwt-public.pem
 *   # For HS256:
 *   secret-base64: <base64-encoded-secret>
 *
 * Notes:
 * - RS256 private key must be PKCS#8 PEM (-----BEGIN PRIVATE KEY-----).
 * - RS256 public key must be X.509 PEM (-----BEGIN PUBLIC KEY-----).
 * - If you only have PKCS#1 private key, convert to PKCS#8:
 *     openssl pkcs8 -topk8 -inform PEM -outform PEM -in key.pkcs1.pem -out key.pkcs8.pem -nocrypt
 */
@Component
public class JWTTokenUtil {
    private static final Logger log = LoggerFactory.getLogger(JWTTokenUtil.class);

    @Value("${jwt.alg:RS256}")
    private String jwtAlg;

    // Normalized algorithm (set in init)
    private String normalizedAlg;

    @Value("${jwt.issuer:centneo}")
    private String issuer;

    @Value("${jwt.access-ttl-ms:18000000}")
    private long accessTtlMs;

    @Value("${jwt.refresh-ttl-ms:86400000}")
    private long refreshTtlMs;

    // RS256 keys (file paths or inline PEM)
    @Value("${jwt.private-key-file:}")
    private String privateKeyFile;

    @Value("${jwt.public-key-file:}")
    private String publicKeyFile;

    // HS secret (base64)
    @Value("${jwt.secret-base64:}")
    private String secretBase64;

    // Cookie name
    @Value("${jwt.cookie-name:jwt_token}")
    private String cookieName;

    // Internal keys
    private Key signingKey;      // For signing (HS secret or RSA private)
    private Key verificationKey; // For verifying (HS secret or RSA public)

    @PostConstruct
    public void init() throws Exception {
        normalizedAlg = (jwtAlg == null) ? "RS256" : jwtAlg.trim().toUpperCase(Locale.ROOT);

        if ("HS256".equals(normalizedAlg)) {
            if (secretBase64 == null || secretBase64.isBlank()) {
                throw new IllegalStateException("HS256 selected but 'jwt.secret-base64' is not configured");
            }
            byte[] keyBytes = Decoders.BASE64.decode(secretBase64.trim());
            if (keyBytes.length < 32) { // 256 bits = 32 bytes
                throw new IllegalStateException("jwt.secret-base64 must decode to at least 256 bits (32 bytes) for HS256");
            }
            signingKey = Keys.hmacShaKeyFor(keyBytes);
            verificationKey = signingKey;
            log.info("JWTTokenUtil initialized in HS256 mode");
        } else if ("RS256".equals(normalizedAlg)) {
            // Load private (signing) key if provided
            if (privateKeyFile != null && !privateKeyFile.isBlank()) {
                try {
                    signingKey = loadPrivateKeyFromPemOrInline(privateKeyFile);
                } catch (Exception e) {
                    // Don't swallow — surface a helpful error
                    throw new IllegalStateException("Failed to load RSA private key from '" + privateKeyFile + "': " + e.getMessage(), e);
                }
            } else {
                log.warn("RS256 selected but 'jwt.private-key-file' not configured. Signing will fail if attempted.");
            }

            if (publicKeyFile != null && !publicKeyFile.isBlank()) {
                try {
                    verificationKey = loadPublicKeyFromPemOrInline(publicKeyFile);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to load RSA public key from '" + publicKeyFile + "': " + e.getMessage(), e);
                }
            } else {
                log.warn("RS256 selected but 'jwt.public-key-file' not configured. Verification will fail.");
            }
            log.info("JWTTokenUtil initialized in RS256 mode");
        } else {
            throw new IllegalStateException("Unsupported jwt.alg: " + jwtAlg + " (allowed: HS256, RS256)");
        }
    }

    // ----------------------------
    // Token generation
    // ----------------------------

    /**
     * Backwards-compatible alias - produces access token.
     */
    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Map GrantedAuthority -> String (authority names) to keep claims JSON-serializable
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());
        claims.put("roles", roles);
        return generateAccessToken(userDetails.getUsername(), claims);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // refresh token may have fewer claims
        return generateRefreshToken(userDetails.getUsername(), claims);
    }

    public String generateAccessToken(String subject, Map<String, Object> extraClaims) {
        return buildToken(subject, extraClaims, accessTtlMs);
    }

    public String generateRefreshToken(String subject, Map<String, Object> extraClaims) {
        return buildToken(subject, extraClaims, refreshTtlMs);
    }

    private String buildToken(String subject, Map<String, Object> extraClaims, long ttlMs) {
        if (signingKey == null) {
            // If algorithm is RS256 and private key absent, signing not possible; for HS256 this shouldn't happen.
            if ("RS256".equalsIgnoreCase(normalizedAlg)) {
                throw new IllegalStateException("Signing key (RSA private) is not configured for RS256");
            } else {
                throw new IllegalStateException("Signing key is not configured");
            }
        }
        long now = Instant.now().toEpochMilli();
        Date iat = new Date(now);
        Date exp = new Date(now + ttlMs);

        JwtBuilder builder = Jwts.builder()
                .setClaims(extraClaims == null ? Map.of() : new HashMap<>(extraClaims))
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(iat)
                .setExpiration(exp);

        SignatureAlgorithm sigAlg = getSignatureAlgorithm();

        return builder.signWith(signingKey, sigAlg).compact();
    }

    // ----------------------------
    // Parsing & validation
    // ----------------------------

    /**
     * Parse token and return Jws<Claims>. Throws JwtException on invalid token.
     */
    public Jws<Claims> parseToken(String token) throws JwtException {
        if (verificationKey == null) {
            throw new IllegalStateException("Verification key not configured");
        }

        JwtParserBuilder pb = Jwts.parserBuilder()
                .setSigningKey(verificationKey)
                .setAllowedClockSkewSeconds(60); // allow small clock skew (60s)

        if (issuer != null && !issuer.isBlank()) {
            pb.requireIssuer(issuer);
        }

        return pb.build().parseClaimsJws(token);
    }

    /**
     * Validate token signature and expiry (no subject check).
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("validateToken failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token for a specific user (subject) and expiry.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("validateToken(user) failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate refresh token existence/signature/expiry (no user check).
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token);
    }

    /**
     * Validate refresh token against UserDetails (subject + expiry).
     */
    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        return validateToken(token, userDetails);
    }

    // ----------------------------
    // Claims helpers
    // ----------------------------

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String getUsernameFromRefreshToken(String token) {
        return getUsernameFromToken(token);
    }

    public Date getExpirationFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, java.util.function.Function<Claims, T> resolver) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims == null ? null : resolver.apply(claims);
        } catch (JwtException e) {
            log.debug("getClaimFromToken failed: {}", e.getMessage());
            return null;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        Jws<Claims> jws = parseToken(token);
        return jws.getBody();
    }

    private boolean isTokenExpired(String token) {
        Date exp = getExpirationFromToken(token);
        return exp != null && exp.before(new Date());
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims c = getAllClaimsFromToken(token);
            Object roles = c.get("roles");
            if (roles instanceof Collection<?>) {
                List<String> out = new ArrayList<>();
                for (Object r : (Collection<?>) roles) out.add(String.valueOf(r));
                return out;
            }
            return roles == null ? Collections.emptyList() : Collections.singletonList(roles.toString());
        } catch (Exception e) {
            log.debug("getRolesFromToken failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ----------------------------
    // Request helpers
    // ----------------------------

    /**
     * Extract token from cookie (jwt cookie-name) or Authorization Bearer header.
     */
    public String extractToken(HttpServletRequest request) {
        if (request == null) return null;

        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (c != null && cookieName.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

    // ----------------------------
    // Key loaders & utils
    // ----------------------------

    private SignatureAlgorithm getSignatureAlgorithm() {
        if ("HS256".equalsIgnoreCase(normalizedAlg)) return SignatureAlgorithm.HS256;
        return SignatureAlgorithm.RS256;
    }

    private Key loadPrivateKeyFromPemOrInline(String pemOrPath) throws Exception {
        String pem = readPemOrInline(pemOrPath);
        return loadPrivateKeyFromPemString(pem);
    }

    private Key loadPublicKeyFromPemOrInline(String pemOrPath) throws Exception {
        String pem = readPemOrInline(pemOrPath);
        return loadPublicKeyFromPemString(pem);
    }

    private static String readPemOrInline(String pemOrPath) throws IOException {
        if (pemOrPath == null) return null;
        pemOrPath = pemOrPath.trim();

        // classpath:resource (e.g., classpath:/keys/jwt.pem or classpath:keys/jwt.pem)
        if (pemOrPath.startsWith("classpath:")) {
            String resource = pemOrPath.substring("classpath:".length());
            if (!resource.startsWith("/")) resource = "/" + resource;
            try (InputStream in = JWTTokenUtil.class.getResourceAsStream(resource)) {
                if (in == null) throw new IOException("Classpath resource not found: " + resource);
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        // file: prefix
        if (pemOrPath.startsWith("file:")) {
            return Files.readString(Path.of(pemOrPath.substring(5)));
        }

        // If path exists on filesystem (works on Windows and Linux)
        Path p = Path.of(pemOrPath);
        if (Files.exists(p)) {
            return Files.readString(p);
        }

        // Otherwise treat as inline PEM text
        return pemOrPath;
    }

    private PrivateKey loadPrivateKeyFromPemString(String pem) throws Exception {
        try {
            byte[] der = base64FromPem(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (PrivateKey) kf.generatePrivate(spec);
        } catch (InvalidKeySpecException ikse) {
            // Helpful error message — PKCS#1 -> PKCS#8 must be converted
            throw new IllegalArgumentException("Failed to load RSA PKCS#8 private key. Ensure private key is PKCS#8 PEM (-----BEGIN PRIVATE KEY-----). "
                    + "If your key is PKCS#1 (-----BEGIN RSA PRIVATE KEY-----) convert it to PKCS#8 with: "
                    + "openssl pkcs8 -topk8 -inform PEM -outform PEM -in key.pkcs1.pem -out key.pkcs8.pem -nocrypt", ikse);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse private key: " + ex.getMessage(), ex);
        }
    }

    private PublicKey loadPublicKeyFromPemString(String pem) throws Exception {
        try {
            byte[] der = base64FromPem(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (PublicKey) kf.generatePublic(spec);
        } catch (InvalidKeySpecException ikse) {
            throw new IllegalArgumentException("Failed to load RSA public key. Ensure public key is X.509 PEM (-----BEGIN PUBLIC KEY-----).", ikse);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse public key: " + ex.getMessage(), ex);
        }
    }

    private static byte[] base64FromPem(String pem) {
        if (pem == null) return new byte[0];
        // strip PEM headers/footers and whitespace
        String cleaned = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }

    // ----------------------------
    // TTL getters (convenience)
    // ----------------------------
    public long getAccessTtlMs() {
        return accessTtlMs;
    }

    public long getRefreshTtlMs() {
        return refreshTtlMs;
    }
}