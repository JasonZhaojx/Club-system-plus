package com.backend.sever.controller;

import com.backend.pojo.vo.FileUploadVO;
import com.backend.sever.common.Result;
import com.backend.sever.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyAuthority('activity:create', 'activity:update', 'coupon:manage', 'system:maintain')")
    public Result<FileUploadVO> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "activity") String scene
    ) {
        return Result.success(fileStorageService.uploadImage(file, scene));
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
