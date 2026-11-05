package com.designer.costume.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.designer.costume.exception.InvalidTokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET =
            Base64.getEncoder().encodeToString(new byte[32]);
    private static final String ISSUER = "designer-costume-api-test";

    private final JwtService jwtService =
            new JwtService(new JwtProperties(SECRET, 60_000, 604_800_000, ISSUER));

    @Test
    void acceptsCorrectSignatureIssuerAndTokenType() {
        String token = jwtService.generate(
                UUID.randomUUID(), JwtTokenType.ACCESS, UUID.randomUUID(), 60_000);

        assertThat(jwtService.parse(token, JwtTokenType.ACCESS).getIssuer()).isEqualTo(ISSUER);
    }

    @Test
    void rejectsWrongSignatureIssuerAndTokenType() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        String otherKeyToken = Jwts.builder()
                .subject(userId.toString())
                .id(tokenId.toString())
                .claim("type", JwtTokenType.ACCESS.name())
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(new byte[] {
                        1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1
                }))
                .compact();
        String wrongIssuerToken = Jwts.builder()
                .subject(userId.toString())
                .id(tokenId.toString())
                .claim("type", JwtTokenType.ACCESS.name())
                .issuer("unexpected-issuer")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();
        String refreshToken = jwtService.generate(
                userId, JwtTokenType.REFRESH, UUID.randomUUID(), 60_000);

        assertThatThrownBy(() -> jwtService.parse(otherKeyToken, JwtTokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> jwtService.parse(wrongIssuerToken, JwtTokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> jwtService.parse(refreshToken, JwtTokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredAccessTokensButAllowsExpiredRefreshTokensForRevocationOnly() {
        String expiredAccess = jwtService.generate(
                UUID.randomUUID(), JwtTokenType.ACCESS, UUID.randomUUID(), -1_000);
        String expiredRefresh = jwtService.generate(
                UUID.randomUUID(), JwtTokenType.REFRESH, UUID.randomUUID(), -1_000);

        assertThatThrownBy(() -> jwtService.parse(expiredAccess, JwtTokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
        assertThat(jwtService.parseForRevocation(expiredRefresh, JwtTokenType.REFRESH).getExpiration())
                .isBefore(new Date());
    }
}
