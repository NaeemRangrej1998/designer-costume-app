package com.designer.costume.service;

import com.designer.costume.dto.request.AdminCreateUserRequest;
import com.designer.costume.dto.request.RegisterRequest;
import com.designer.costume.dto.response.UserResponse;
import com.designer.costume.entity.role.RoleEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.exception.ResourceConflictException;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.exception.BadRequestException;
import com.designer.costume.repository.role.RoleRepository;
import com.designer.costume.repository.user.UserRepository;
import com.designer.costume.repository.user.UserRolePermissionGrantRepository;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import com.designer.costume.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper userResponseMapper;
    private final UserRolePermissionGrantRepository permissionGrantRepository;
    private final AdminActivityService activityService;

    public UserProvisioningService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            UserResponseMapper userResponseMapper,
            UserRolePermissionGrantRepository permissionGrantRepository,
            AdminActivityService activityService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userResponseMapper = userResponseMapper;
        this.permissionGrantRepository = permissionGrantRepository;
        this.activityService = activityService;
    }

    @Transactional
    public UserResponse registerCustomer(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email);
        RoleEntity customer = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is missing from database"));
        UserEntity user = createUser(
                request.fullName(), email, request.phoneNumber(), request.password(), Set.of(customer));
        UserEntity savedUser = userRepository.saveAndFlush(user);
        permissionGrantRepository.snapshotCurrentRolePermissions(savedUser.getId());
        return userResponseMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse createAdminUser(AdminCreateUserRequest request, CustomUserDetails actor) {
        Set<String> normalizedNames = request.roles().stream()
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (normalizedNames.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        if (normalizedNames.contains("SUPER_ADMIN") && !actor.getRoleNames().contains("SUPER_ADMIN")) {
            throw new AccessDeniedException("Only a super administrator may assign SUPER_ADMIN");
        }

        Set<RoleEntity> roles = normalizedNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", name)))
                .collect(Collectors.toSet());
        Set<String> assignedPermissions = roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());
        if (!actor.getPermissionNames().containsAll(assignedPermissions)) {
            throw new AccessDeniedException("Cannot assign permissions the current user does not have");
        }

        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email);
        UserEntity user = createUser(
                request.fullName(), email, request.phoneNumber(), request.password(), roles);
        UserEntity savedUser = userRepository.saveAndFlush(user);
        permissionGrantRepository.snapshotCurrentRolePermissions(savedUser.getId());
        activityService.record("CREATED", "USER", savedUser.getId());
        return userResponseMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getSafeUser(String email) {
        UserEntity user = userRepository.findWithAuthoritiesByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return userResponseMapper.toResponse(user);
    }

    private UserEntity createUser(
            String fullName, String email, String phone, String rawPassword, Set<RoleEntity> roles) {
        UserEntity user = new UserEntity();
        user.setFullName(fullName.trim());
        user.setEmail(email);
        user.setPhoneNumber(phone == null || phone.isBlank() ? null : phone.trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        user.setRoles(roles);
        return user;
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResourceConflictException("An account with this email already exists");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
