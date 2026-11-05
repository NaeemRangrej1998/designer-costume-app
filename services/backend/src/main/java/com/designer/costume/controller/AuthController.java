package com.designer.costume.controller;

import com.designer.costume.dto.request.ForgotPasswordRequest;
import com.designer.costume.dto.request.LoginRequest;
import com.designer.costume.dto.request.RefreshTokenRequest;
import com.designer.costume.dto.request.RegisterRequest;
import com.designer.costume.dto.request.ResetPasswordRequest;
import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.ForgotPasswordResponse;
import com.designer.costume.dto.response.TokenResponse;
import com.designer.costume.dto.response.UserResponse;
import com.designer.costume.security.AuthCookieSupport;
import com.designer.costume.security.CustomUserDetails;
import com.designer.costume.security.TokenService;
import com.designer.costume.service.AuthenticationService;
import com.designer.costume.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final AuthCookieSupport cookieSupport;
    private final TokenService tokenService;

    public AuthController(
            AuthenticationService authenticationService,
            PasswordResetService passwordResetService,
            AuthCookieSupport cookieSupport,
            TokenService tokenService) {
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
        this.cookieSupport = cookieSupport;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        boolean webClient = cookieSupport.isWebClient(httpRequest);
        TokenResponse tokens = authenticationService.register(request);
        return ApiResponse.success("Account created successfully",
                forClient(tokens, webClient, httpResponse));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        boolean webClient = cookieSupport.isWebClient(httpRequest);
        TokenResponse tokens = authenticationService.login(request.email(), request.password());
        return ApiResponse.success("Login successful", forClient(tokens, webClient, httpResponse));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            @Valid @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        boolean webClient = cookieSupport.isWebClient(httpRequest);
        String suppliedToken = request == null ? null : request.refreshToken();
        String refreshToken = cookieSupport.refreshToken(httpRequest, suppliedToken, webClient);
        TokenResponse tokens = authenticationService.refresh(refreshToken);
        return ApiResponse.success("Access token refreshed",
                forClient(tokens, webClient, httpResponse));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        boolean webClient = cookieSupport.isWebClient(httpRequest);
        String suppliedToken = request == null ? null : request.refreshToken();
        String refreshToken = cookieSupport.refreshToken(httpRequest, suppliedToken, webClient);
        authenticationService.logout(refreshToken);
        cookieSupport.clearSessionCookies(httpResponse);
        return ApiResponse.success("Logout successful", null);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserResponse> currentUser(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(
                "Current user fetched successfully",
                authenticationService.currentUser(principal.getUsername()));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.success(
                "Password reset request received",
                passwordResetService.requestReset(request.email()));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request);
        return ApiResponse.success("Password reset successfully", null);
    }

    private TokenResponse forClient(
            TokenResponse tokens, boolean webClient, HttpServletResponse response) {
        if (!webClient) {
            return tokens;
        }
        cookieSupport.writeSessionCookies(
                response, tokens.refreshToken(), tokenService.refreshExpirationSeconds());
        return new TokenResponse(
                tokens.accessToken(),
                null,
                tokens.tokenType(),
                tokens.expiresInSeconds(),
                tokens.user());
    }
}
