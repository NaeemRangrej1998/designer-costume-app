package com.designer.costume.service;

import com.designer.costume.dto.request.AdminRoleRequest;
import com.designer.costume.dto.response.AdminPermissionResponse;
import com.designer.costume.dto.response.AdminRoleResponse;
import com.designer.costume.entity.permission.PermissionEntity;
import com.designer.costume.entity.role.RoleEntity;
import com.designer.costume.exception.BadRequestException;
import com.designer.costume.exception.ResourceConflictException;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.permission.PermissionRepository;
import com.designer.costume.repository.role.RoleRepository;
import com.designer.costume.repository.user.UserRolePermissionGrantRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRolePermissionGrantRepository permissionGrantRepository;
    private final AdminActivityService activityService;

    public AdminRoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRolePermissionGrantRepository permissionGrantRepository,
            AdminActivityService activityService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.permissionGrantRepository = permissionGrantRepository;
        this.activityService = activityService;
    }

    public java.util.List<AdminRoleResponse> getRoles() {
        return roleRepository.findAllByOrderByNameAsc().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    public java.util.List<AdminPermissionResponse> getPermissions() {
        return permissionRepository.findAllByOrderByModuleAscActionAsc().stream()
                .map(permission -> new AdminPermissionResponse(
                        permission.getId(),
                        permission.getName(),
                        permission.getDescription(),
                        permission.getModule(),
                        permission.getAction()))
                .toList();
    }

    @Transactional
    public AdminRoleResponse createRole(AdminRoleRequest request) {
        String name = normalizeRoleName(request.name());
        ensureRoleNameAvailable(name, null);
        RoleEntity role = new RoleEntity();
        applyRole(role, name, request);
        RoleEntity savedRole = roleRepository.save(role);
        activityService.record("CREATED", "ROLE", savedRole.getId());
        return toRoleResponse(savedRole);
    }

    @Transactional
    public AdminRoleResponse updateRole(UUID id, AdminRoleRequest request) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        String name = normalizeRoleName(request.name());
        if (isReservedRole(role.getName()) && !role.getName().equals(name)) {
            throw new BadRequestException("Built-in role names cannot be changed");
        }
        ensureRoleNameAvailable(name, id);
        applyRole(role, name, request);
        RoleEntity savedRole = roleRepository.saveAndFlush(role);
        permissionGrantRepository.removePermissionsNoLongerGrantedByRole(savedRole.getId());
        activityService.record("UPDATED", "ROLE", savedRole.getId());
        return toRoleResponse(savedRole);
    }

    private void applyRole(RoleEntity role, String name, AdminRoleRequest request) {
        Set<String> permissionNames = request.permissions().stream()
                .map(permission -> permission.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        Set<PermissionEntity> permissions = permissionRepository
                .findAllByNameIn(permissionNames).stream().collect(Collectors.toSet());
        if (permissions.size() != permissionNames.size()) {
            throw new BadRequestException("One or more permissions do not exist");
        }
        role.setName(name);
        role.setDescription(request.description() == null || request.description().isBlank()
                ? null
                : request.description().trim());
        role.setPermissions(permissions);
    }

    private void ensureRoleNameAvailable(String name, UUID currentId) {
        roleRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResourceConflictException("Role name is already in use");
                });
    }

    private AdminRoleResponse toRoleResponse(RoleEntity role) {
        return new AdminRoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream()
                        .map(PermissionEntity::getName)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    private String normalizeRoleName(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isReservedRole(String roleName) {
        return Set.of("CUSTOMER", "ADMIN", "SUPER_ADMIN").contains(roleName);
    }
}
