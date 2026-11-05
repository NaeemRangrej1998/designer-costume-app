package com.designer.costume.service;

import com.designer.costume.dto.request.RegisterRequest;
import com.designer.costume.dto.response.TokenResponse;
import com.designer.costume.dto.response.UserResponse;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.user.UserRepository;
import com.designer.costume.security.JwtService;
import com.designer.costume.security.JwtTokenType;
import com.designer.costume.security.TokenService;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserProvisioningService userProvisioningService;
    private final UserResponseMapper userResponseMapper;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            UserProvisioningService userProvisioningService,
            UserResponseMapper userResponseMapper,
            TokenService tokenService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userProvisioningService = userProvisioningService;
        this.userResponseMapper = userResponseMapper;
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        userProvisioningService.registerCustomer(request);
        UserEntity user = findUser(request.email());
        return issueFor(user);
    }

    @Transactional
    public TokenResponse login(String email, String password) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email.trim(), password));
        UserEntity user = findUser(email);
        UserEntity lockedUser = userRepository.findByIdForUpdate(user.getId())
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(password, lockedUser.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return issueFor(findUser(email));
    }

    public TokenResponse refresh(String rawRefreshToken) {
        TokenService.RotatedToken rotated = tokenService.rotate(rawRefreshToken);
        UserEntity user = findUserById(rotated.userId());
        return new TokenResponse(
                rotated.accessToken(),
                rotated.refreshToken(),
                "Bearer",
                tokenService.accessExpirationSeconds(),
                userResponseMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return userProvisioningService.getSafeUser(email);
    }

    public void logout(String refreshToken) {
        tokenService.revoke(refreshToken);
    }

    private TokenResponse issueFor(UserEntity user) {
        String refreshToken = tokenService.createRefreshToken(user);
        String accessToken = jwtService.generate(
                user.getId(),
                JwtTokenType.ACCESS,
                UUID.randomUUID(),
                jwtService.accessExpirationMillis());
        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                tokenService.accessExpirationSeconds(),
                userResponseMapper.toResponse(user));
    }

    private UserEntity findUser(String email) {
        return userRepository.findWithAuthoritiesByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    private UserEntity findUserById(UUID id) {
        return userRepository.findWithAuthoritiesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
