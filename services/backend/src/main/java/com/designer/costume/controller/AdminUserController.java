package com.designer.costume.controller;

import com.designer.costume.dto.request.AdminCreateUserRequest;
import com.designer.costume.dto.request.AdminUpdateUserRequest;
import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.PageResponse;
import com.designer.costume.dto.response.UserResponse;
import com.designer.costume.security.CustomUserDetails;
import com.designer.costume.service.AdminUserManagementService;
import com.designer.costume.service.UserProvisioningService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@Validated
public class AdminUserController {

    private final UserProvisioningService userProvisioningService;
    private final AdminUserManagementService userManagementService;

    public AdminUserController(
            UserProvisioningService userProvisioningService,
            AdminUserManagementService userManagementService) {
        this.userProvisioningService = userProvisioningService;
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.success("Users fetched successfully",
                userManagementService.getUsers(page, size, search, active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.success("User fetched successfully", userManagementService.getUser(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success(
                "User created successfully",
                userProvisioningService.createAdminUser(request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ApiResponse.success("User updated successfully",
                userManagementService.updateUser(id, request, actor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> deactivateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails actor) {
        userManagementService.deactivateUser(id, actor);
        return ApiResponse.success("User deactivated successfully", null);
    }
}
