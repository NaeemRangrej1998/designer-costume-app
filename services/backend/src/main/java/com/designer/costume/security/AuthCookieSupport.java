package com.designer.costume.security;

import com.designer.costume.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieSupport {

    public static final String REFRESH_COOKIE = "refresh_token";
    public static final String CSRF_COOKIE = "csrf_token";
    public static final String CSRF_HEADER = "X-CSRF-TOKEN";
    public static final String CLIENT_PLATFORM_HEADER = "X-Client-Platform";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final boolean secure;
    private final Set<String> allowedOrigins;

    public AuthCookieSupport(
            @Value("${app.auth.cookie-secure}") boolean secure,
            @Value("${app.cors.allowed-origins}") String origins) {
        this.secure = secure;
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public boolean isWebClient(HttpServletRequest request) {
        validateOrigin(request);
        String platform = request.getHeader(CLIENT_PLATFORM_HEADER);
        if (platform == null || platform.isBlank() || platform.equalsIgnoreCase("web")) {
            return true;
        }
        if (platform.equalsIgnoreCase("mobile")) {
            return false;
        }
        throw new BadRequestException("Unsupported client platform");
    }

    public String refreshToken(HttpServletRequest request, String bodyToken, boolean webClient) {
        if (webClient) {
            validateCsrf(request);
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (var cookie : cookies) {
                    if (REFRESH_COOKIE.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            throw new com.designer.costume.exception.InvalidTokenException();
        }
        if (bodyToken == null || bodyToken.isBlank()) {
            throw new com.designer.costume.exception.InvalidTokenException();
        }
        return bodyToken;
    }

    public void writeSessionCookies(HttpServletResponse response, String refreshToken, long maxAge) {
        String csrfToken = csrfToken();
        add(response, cookie(REFRESH_COOKIE, refreshToken, true, maxAge, "/api/v1/auth"));
        add(response, cookie(CSRF_COOKIE, "", false, 0, "/api/v1/auth"));
        add(response, cookie(CSRF_COOKIE, csrfToken, false, maxAge, "/"));
    }

    public void clearSessionCookies(HttpServletResponse response) {
        add(response, cookie(REFRESH_COOKIE, "", true, 0, "/api/v1/auth"));
        add(response, cookie(CSRF_COOKIE, "", false, 0, "/api/v1/auth"));
        add(response, cookie(CSRF_COOKIE, "", false, 0, "/"));
    }

    public void validateCsrf(HttpServletRequest request) {
        String header = request.getHeader(CSRF_HEADER);
        String cookieValue = null;
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (var cookie : cookies) {
                if (CSRF_COOKIE.equals(cookie.getName())) {
                    cookieValue = cookie.getValue();
                    break;
                }
            }
        }
        if (header == null || cookieValue == null || !MessageDigest.isEqual(
                header.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                cookieValue.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new CsrfValidationException();
        }
    }

    private void validateOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !allowedOrigins.contains(origin)) {
            throw new CsrfValidationException();
        }
    }

    private ResponseCookie cookie(
            String name, String value, boolean httpOnly, long maxAge, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private String csrfToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
