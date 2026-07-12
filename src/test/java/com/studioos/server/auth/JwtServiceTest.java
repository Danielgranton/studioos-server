package com.studioos.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.studioos.server.shared.enums.Role;
import com.studioos.server.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtService, "expiration", 60000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 120000L);
    }

    @Test
    void generatesDistinctTokenTypes() {
        User user = User.builder().id(1).email("user@example.com").name("User").role(Role.USER).build();

        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractTokenType(access)).isEqualTo(JwtService.TOKEN_TYPE_ACCESS);
        assertThat(jwtService.extractTokenType(refresh)).isEqualTo(JwtService.TOKEN_TYPE_REFRESH);
        assertThat(jwtService.isTokenOfType(access, JwtService.TOKEN_TYPE_REFRESH)).isFalse();
    }
}
