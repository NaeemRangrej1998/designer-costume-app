package com.designer.costume.controller;

import com.designer.costume.dto.request.AdminCreateUserRequest;
import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.UserResponse;
import com.designer.costume.security.CustomUserDetails;
import com.designer.costume.service.UserProvisioningService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserProvisioningService userProvisioningService;

    public AdminUserController(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
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
}
