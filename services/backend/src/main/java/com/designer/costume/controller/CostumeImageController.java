package com.designer.costume.controller;

import com.designer.costume.dto.response.ApiResponse;
import com.designer.costume.dto.response.CostumeImageResponse;
import com.designer.costume.service.ImageManagementService;
import com.designer.costume.service.ImageManagementService.StoredImageContent;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class CostumeImageController {

    private final ImageManagementService imageService;

    public CostumeImageController(ImageManagementService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/costumes/{costumeId}/images")
    public ApiResponse<List<CostumeImageResponse>> getCostumeImages(
            @PathVariable UUID costumeId) {
        return ApiResponse.success("Costume images fetched successfully",
                imageService.getPublicCostumeImages(costumeId));
    }

    @GetMapping("/costumes/{costumeId}/images/{imageId}/content")
    public ResponseEntity<Resource> getCostumeImageContent(
            @PathVariable UUID costumeId, @PathVariable UUID imageId) {
        StoredImageContent image = imageService.getCostumeImageContent(costumeId, imageId, true);
        MediaType contentType = MediaType.parseMediaType(image.mimeType());
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.noCache().cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }

    @GetMapping("/admin/costumes/{costumeId}/images")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('COSTUME_VIEW')")
    public ApiResponse<List<CostumeImageResponse>> getAdminCostumeImages(
            @PathVariable UUID costumeId) {
        return ApiResponse.success("Costume images fetched successfully",
                imageService.getCostumeImages(costumeId));
    }

    @GetMapping("/admin/costumes/{costumeId}/images/{imageId}/content")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('COSTUME_VIEW')")
    public ResponseEntity<Resource> getAdminCostumeImageContent(
            @PathVariable UUID costumeId, @PathVariable UUID imageId) {
        StoredImageContent image = imageService.getCostumeImageContent(costumeId, imageId, false);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mimeType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }

    @PostMapping(value = "/admin/costumes/{costumeId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('COSTUME_UPDATE')")
    public ApiResponse<CostumeImageResponse> uploadCostumeImage(
            @PathVariable UUID costumeId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Costume image uploaded successfully",
                imageService.uploadCostumeImage(costumeId, file));
    }

    @PutMapping("/admin/costumes/{costumeId}/images/order")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('COSTUME_UPDATE')")
    public ApiResponse<List<CostumeImageResponse>> reorderCostumeImages(
            @PathVariable UUID costumeId, @Valid @RequestBody ReorderRequest request) {
        return ApiResponse.success("Costume images reordered successfully",
                imageService.reorderCostumeImages(costumeId, request.imageIds()));
    }

    @PutMapping("/admin/costumes/{costumeId}/images/{imageId}/primary")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('COSTUME_UPDATE')")
    public ApiResponse<CostumeImageResponse> setPrimaryCostumeImage(
            @PathVariable UUID costumeId, @PathVariable UUID imageId) {
        return ApiResponse.success("Primary costume image updated successfully",
                imageService.setPrimaryCostumeImage(costumeId, imageId));
    }

    @DeleteMapping("/admin/costumes/{costumeId}/images/{imageId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('COSTUME_UPDATE')")
    public ApiResponse<Void> deleteCostumeImage(
            @PathVariable UUID costumeId, @PathVariable UUID imageId) {
        imageService.deleteCostumeImage(costumeId, imageId);
        return ApiResponse.success("Costume image deleted successfully", null);
    }

    public record ReorderRequest(@NotEmpty List<UUID> imageIds) {
    }
}
