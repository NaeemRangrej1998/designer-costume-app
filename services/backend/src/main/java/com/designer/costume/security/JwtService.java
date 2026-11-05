package com.designer.costume.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.secret());
        } catch (IllegalArgumentException exception) {
            keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret must provide at least 256 bits");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generate(UUID userId, JwtTokenType type, UUID tokenId, long expirationMillis) {
        Instant issuedAt = Instant.now();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .id(tokenId.toString())
                .claim("type", type.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(expirationMillis)));
        if (properties.issuer() != null && !properties.issuer().isBlank()) {
            builder.issuer(properties.issuer());
        }
        return builder.signWith(signingKey).compact();
    }

    public Claims parse(String token, JwtTokenType expectedType) {
        return parse(token, expectedType, false);
    }

    public Claims parseForRevocation(String token, JwtTokenType expectedType) {
        return parse(token, expectedType, true);
    }

    private Claims parse(String token, JwtTokenType expectedType, boolean allowExpired) {
        try {
            var parser = Jwts.parser().verifyWith(signingKey);
            if (properties.issuer() != null && !properties.issuer().isBlank()) {
                parser.requireIssuer(properties.issuer());
            }
            Claims claims = parser.build().parseSignedClaims(token).getPayload();
            return validateClaims(claims, expectedType);
        } catch (ExpiredJwtException exception) {
            if (allowExpired) {
                try {
                    return validateClaims(exception.getClaims(), expectedType);
                } catch (JwtException | IllegalArgumentException validationException) {
                    throw new com.designer.costume.exception.InvalidTokenException();
                }
            }
            throw new com.designer.costume.exception.InvalidTokenException();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new com.designer.costume.exception.InvalidTokenException();
        }
    }

    private Claims validateClaims(Claims claims, JwtTokenType expectedType) {
        if (!expectedType.name().equals(claims.get("type", String.class))) {
            throw new JwtException("Unexpected token type");
        }
        String subject = claims.getSubject();
        String tokenId = claims.getId();
        if (subject == null || tokenId == null) {
            throw new JwtException("Required token claims are missing");
        }
        UUID.fromString(subject);
        UUID.fromString(tokenId);
        return claims;
    }

    public long accessExpirationMillis() {
        return properties.accessTokenExpiration();
    }

    public long refreshExpirationMillis() {
        return properties.refreshTokenExpiration();
    }
}
