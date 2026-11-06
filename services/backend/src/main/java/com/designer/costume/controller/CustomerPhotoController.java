package com.designer.costume.controller;

import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.UserPhotoResponse;
import com.designer.costume.security.CustomUserDetails;
import com.designer.costume.service.ImageManagementService;
import com.designer.costume.service.ImageManagementService.StoredImageContent;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/customer/photos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPhotoController {

    private final ImageManagementService imageService;

    public CustomerPhotoController(ImageManagementService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ApiResponse<List<UserPhotoResponse>> getPhotos(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success("Photos fetched successfully",
                imageService.getUserPhotos(principal.getId()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserPhotoResponse> uploadPhoto(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Photo uploaded successfully",
                imageService.uploadUserPhoto(principal.getId(), file));
    }

    @GetMapping("/{photoId}/content")
    public ResponseEntity<Resource> getPhotoContent(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID photoId) {
        StoredImageContent image = imageService.getUserPhotoContent(principal.getId(), photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mimeType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }

    @DeleteMapping("/{photoId}")
    public ApiResponse<Void> deletePhoto(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID photoId) {
        imageService.deleteUserPhoto(principal.getId(), photoId);
        return ApiResponse.success("Photo deleted successfully", null);
    }
}
