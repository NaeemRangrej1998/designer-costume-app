package com.designer.costume.service;

import com.designer.costume.dto.request.AdminUpdateUserRequest;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.dto.response.UserResponse;
import com.designer.costume.entity.role.RoleEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.exception.BadRequestException;
import com.designer.costume.exception.ResourceConflictException;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.role.RoleRepository;
import com.designer.costume.repository.user.RefreshTokenRepository;
import com.designer.costume.repository.user.UserRepository;
import com.designer.costume.repository.user.UserRolePermissionGrantRepository;
import com.designer.costume.security.CustomUserDetails;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminUserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRolePermissionGrantRepository permissionGrantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserResponseMapper userResponseMapper;
    private final AdminActivityService activityService;

    public AdminUserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRolePermissionGrantRepository permissionGrantRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserResponseMapper userResponseMapper,
            AdminActivityService activityService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionGrantRepository = permissionGrantRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userResponseMapper = userResponseMapper;
        this.activityService = activityService;
    }

    public PageResponse<UserResponse> getUsers(int page, int size, String search, Boolean active) {
        Page<UserEntity> result = userRepository.findAdminUsers(
                search == null || search.isBlank() ? "" : search.trim(),
                active,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fullName")));
        return PageResponse.from(result.map(userResponseMapper::toResponse));
    }

    public UserResponse getUser(UUID id) {
        return userResponseMapper.toResponse(findUser(id));
    }

    @Transactional
    public UserResponse updateUser(UUID id, AdminUpdateUserRequest request, CustomUserDetails actor) {
        if (id.equals(actor.getId()) && !request.active()) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        UserEntity user = findUser(id);
        Set<UUID> existingRoleIds = user.getRoles().stream()
                .map(RoleEntity::getId)
                .collect(Collectors.toSet());
        Set<RoleEntity> roles = resolveRoles(request.roles(), actor);
        Set<UUID> newlyAssignedRoleIds = roles.stream()
                .map(RoleEntity::getId)
                .filter(roleId -> !existingRoleIds.contains(roleId))
                .collect(Collectors.toSet());
        ensureProtectedUserCanBeManaged(user, actor);
        ensureActiveSuperAdminRemains(user, roles, request.active());
        ensureEmailAvailable(request.email(), id);

        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPhoneNumber(request.phoneNumber() == null || request.phoneNumber().isBlank()
                ? null
                : request.phoneNumber().trim());
        user.setRoles(roles);
        user.setActive(request.active());

        UserEntity saved = userRepository.saveAndFlush(user);
        newlyAssignedRoleIds.forEach(roleId ->
                permissionGrantRepository.snapshotRolePermissions(saved.getId(), roleId));
        if (!saved.isActive()) {
            refreshTokenRepository.revokeAllForUser(saved.getId(), Instant.now());
        }
        activityService.record("UPDATED", "USER", saved.getId());
        return userResponseMapper.toResponse(saved);
    }

    @Transactional
    public void deactivateUser(UUID id, CustomUserDetails actor) {
        if (id.equals(actor.getId())) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        UserEntity user = findUser(id);
        ensureProtectedUserCanBeManaged(user, actor);
        user.setActive(false);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        activityService.record("DEACTIVATED", "USER", user.getId());
    }

    private Set<RoleEntity> resolveRoles(Set<String> requestedRoles, CustomUserDetails actor) {
        Set<String> normalizedNames = requestedRoles.stream()
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (normalizedNames.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        if (normalizedNames.contains("SUPER_ADMIN")
                && !actor.getRoleNames().contains("SUPER_ADMIN")) {
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
        return roles;
    }

    private void ensureProtectedUserCanBeManaged(UserEntity user, CustomUserDetails actor) {
        boolean targetIsSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()));
        if (targetIsSuperAdmin && !actor.getRoleNames().contains("SUPER_ADMIN")) {
            throw new AccessDeniedException("Only a super administrator may manage another super administrator");
        }
    }

    private void ensureActiveSuperAdminRemains(
            UserEntity user, Set<RoleEntity> newRoles, boolean active) {
        boolean currentlySuperAdmin = user.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()));
        boolean remainsSuperAdmin = newRoles.stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()));
        if (currentlySuperAdmin
                && user.isActive()
                && (!active || !remainsSuperAdmin)
                && userRepository.countActiveUsersWithRole("SUPER_ADMIN") <= 1) {
            throw new BadRequestException("The last active super administrator cannot be deactivated or demoted");
        }
    }

    private void ensureEmailAvailable(String email, UUID currentId) {
        userRepository.findByEmailIgnoreCase(email.trim())
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResourceConflictException("An account with this email already exists");
                });
    }

    private UserEntity findUser(UUID id) {
        return userRepository.findWithAuthoritiesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
