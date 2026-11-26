package com.homeschool.platform.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatedTokenContainsUserAndVersionClaims() {
        JwtProperties properties = new JwtProperties(
                "home-school-platform-test",
                60,
                "ZGV2LW9ubHktY2hhbmdlLW1lLWluLXByb2R1Y3Rpb24tY29kZXgtc2VjcmV0LTIwMjY=");
        JwtService jwtService = new JwtService(properties);
        UserPrincipal principal = new UserPrincipal(
                1L,
                "admin",
                "unused",
                "系统管理员",
                "ENABLED",
                2,
                List.of("ADMIN"));

        String token = jwtService.generateToken(principal);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(((Number) claims.get("uid")).longValue()).isEqualTo(1L);
        assertThat(((Number) claims.get("ver")).intValue()).isEqualTo(2);
        assertThat(claims.get("roles", List.class)).containsExactly("ADMIN");
    }
}

