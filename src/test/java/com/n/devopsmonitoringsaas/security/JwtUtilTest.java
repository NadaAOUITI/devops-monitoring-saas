package com.n.devopsmonitoringsaas.security;

import com.n.devopsmonitoringsaas.entity.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET = "test-jwt-secret-key-min-256-bits-for-hs256-algorithm";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
    }

    @Nested
    @DisplayName("generateToken & validateAndGetClaims")
    class TokenLifecycle {

        @Test
        @DisplayName("round-trips userId, tenantId, and role")
        void roundTripsClaims() {
            String token = jwtUtil.generateToken(42L, 7L, UserRole.ADMIN);

            JwtUtil.JwtClaims claims = jwtUtil.validateAndGetClaims(token);

            assertThat(claims).isNotNull();
            assertThat(claims.userId()).isEqualTo(42L);
            assertThat(claims.tenantId()).isEqualTo(7L);
            assertThat(claims.role()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("returns null when token is expired")
        void expiredToken_returnsNull() {
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            Date past = new Date(System.currentTimeMillis() - 60_000);
            String expired = Jwts.builder()
                    .subject("1")
                    .claim("tenantId", 2L)
                    .claim("role", "OWNER")
                    .issuedAt(new Date(past.getTime() - 60_000))
                    .expiration(past)
                    .signWith(key)
                    .compact();

            assertThat(jwtUtil.validateAndGetClaims(expired)).isNull();
        }

        @Test
        @DisplayName("returns null when signature is invalid")
        void invalidSignature_returnsNull() {
            SecretKey otherKey = Keys.hmacShaKeyFor("other-secret-key-min-256-bits-for-hs256-algorithm!!".getBytes(StandardCharsets.UTF_8));
            String bad = Jwts.builder()
                    .subject("1")
                    .claim("tenantId", 2L)
                    .claim("role", "OWNER")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(otherKey)
                    .compact();

            assertThat(jwtUtil.validateAndGetClaims(bad)).isNull();
        }

        @Test
        @DisplayName("returns null for malformed token string")
        void malformed_returnsNull() {
            assertThat(jwtUtil.validateAndGetClaims("not-a-jwt")).isNull();
        }
    }

    @Nested
    @DisplayName("claim extraction (via JwtClaims)")
    class ClaimsMapping {

        @Test
        @DisplayName("subject maps to user id")
        void userId() {
            String token = jwtUtil.generateToken(99L, 1L, UserRole.MEMBER);
            JwtUtil.JwtClaims c = jwtUtil.validateAndGetClaims(token);
            assertThat(c.userId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("tenantId claim")
        void tenantId() {
            String token = jwtUtil.generateToken(1L, 12345L, UserRole.OWNER);
            assertThat(jwtUtil.validateAndGetClaims(token).tenantId()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("role claim")
        void role() {
            String token = jwtUtil.generateToken(1L, 1L, UserRole.MEMBER);
            assertThat(jwtUtil.validateAndGetClaims(token).role()).isEqualTo(UserRole.MEMBER);
        }
    }

    @Nested
    @DisplayName("expiry behaviour")
    class Expiry {

        @Test
        @DisplayName("ExpiredJwtException is handled and yields null claims")
        void expiredYieldsNull() {
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String expired = Jwts.builder()
                    .subject("1")
                    .claim("tenantId", 1L)
                    .claim("role", "OWNER")
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(key)
                    .compact();

            assertThat(jwtUtil.validateAndGetClaims(expired)).isNull();
        }
    }
}
