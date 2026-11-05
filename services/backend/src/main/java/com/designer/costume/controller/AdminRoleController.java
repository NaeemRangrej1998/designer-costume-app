package com.designer.costume.controller;

import com.designer.costume.dto.request.AdminRoleRequest;
import com.designer.costume.dto.response.AdminPermissionResponse;
import com.designer.costume.dto.response.AdminRoleResponse;
import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.service.AdminRoleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminRoleController {

    private final AdminRoleService roleService;

    public AdminRoleController(AdminRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    public ApiResponse<List<AdminRoleResponse>> getRoles() {
        return ApiResponse.success("Roles fetched successfully", roleService.getRoles());
    }

    @PostMapping("/roles")
    public ApiResponse<AdminRoleResponse> createRole(@Valid @RequestBody AdminRoleRequest request) {
        return ApiResponse.success("Role created successfully", roleService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<AdminRoleResponse> updateRole(
            @PathVariable UUID id, @Valid @RequestBody AdminRoleRequest request) {
        return ApiResponse.success("Role updated successfully", roleService.updateRole(id, request));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<AdminPermissionResponse>> getPermissions() {
        return ApiResponse.success("Permissions fetched successfully", roleService.getPermissions());
    }
}
