package com.designer.costume.service;

import com.designer.costume.dto.response.UserResponse;
import com.designer.costume.entity.role.RoleEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.repository.user.UserRolePermissionGrantRepository;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    private final UserRolePermissionGrantRepository permissionGrantRepository;

    public UserResponseMapper(UserRolePermissionGrantRepository permissionGrantRepository) {
        this.permissionGrantRepository = permissionGrantRepository;
    }

    public UserResponse toResponse(UserEntity user) {
        var roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        var permissions = permissionGrantRepository.findEffectivePermissions(user.getId());
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getProfileImage(),
                user.isActive(),
                roles,
                permissions);
    }
}
