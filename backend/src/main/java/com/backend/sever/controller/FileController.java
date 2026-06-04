package com.backend.sever.controller;

import com.backend.pojo.vo.FileUploadVO;
import com.backend.sever.common.Result;
import com.backend.sever.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/files")
public class FileController {
    private static final String IMAGE_PROXY_PREFIX = "/files/images/";

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/images")
    @PreAuthorize("hasAnyAuthority('activity:create', 'activity:update', 'activity:review', 'coupon:manage', 'system:maintain')")
    public Result<FileUploadVO> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "activity") String scene,
            Authentication authentication
    ) {
        if ("activity-review".equals(scene) && !canManageActivityReview(authentication)) {
            throw new org.springframework.security.access.AccessDeniedException("Only activity reviewers can upload activity review images");
        }
        return Result.success(fileStorageService.uploadImage(file, scene));
    }

    private boolean canManageActivityReview(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "activity:review".equals(authority.getAuthority())
                        || "system:maintain".equals(authority.getAuthority()));
    }

    @PostMapping("/avatars")
    @PreAuthorize("isAuthenticated()")
    public Result<FileUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(fileStorageService.uploadImage(file, "avatar"));
    }

    @GetMapping("/images/**")
    public ResponseEntity<byte[]> getImage(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        int index = requestUri.indexOf(IMAGE_PROXY_PREFIX);
        String objectName = index < 0 ? "" : requestUri.substring(index + IMAGE_PROXY_PREFIX.length());
        FileStorageService.StoredFile storedFile = fileStorageService.getImage(objectName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(storedFile.content());
    }
}
