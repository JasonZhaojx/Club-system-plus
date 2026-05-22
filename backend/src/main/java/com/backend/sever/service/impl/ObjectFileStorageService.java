package com.backend.sever.service.impl;

import com.backend.pojo.vo.FileUploadVO;
import com.backend.sever.config.FileStorageProperties;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.FileStorageService;
import com.backend.sever.storage.ObjectStorageClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ObjectFileStorageService implements FileStorageService {
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final Set<String> ALLOWED_SCENES = Set.of("activity", "avatar", "coupon");

    private final ObjectStorageClient objectStorageClient;
    private final FileStorageProperties properties;

    public ObjectFileStorageService(ObjectStorageClient objectStorageClient, FileStorageProperties properties) {
        this.objectStorageClient = objectStorageClient;
        this.properties = properties;
    }

    @Override
    public FileUploadVO uploadImage(MultipartFile file, String scene) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能为空");
        }
        if (file.getSize() > properties.getMaxImageSizeBytes()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能超过 " + properties.getMaxImageSizeBytes() / 1024 / 1024 + "MB");
        }
        String normalizedScene = normalizeScene(scene);
        try {
            byte[] content = file.getBytes();
            String contentType = detectImageContentType(content);
            String objectName = buildObjectName(normalizedScene, contentType);
            try (InputStream inputStream = new ByteArrayInputStream(content)) {
                objectStorageClient.putObject(objectName, inputStream, content.length, contentType);
            }
            return new FileUploadVO("/api/files/images/" + objectName, objectName, contentType, content.length);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片上传失败");
        }
    }

    @Override
    public StoredFile getImage(String objectName) {
        String normalizedObjectName = normalizeObjectName(objectName);
        try {
            ObjectStorageClient.StoredObject storedObject = objectStorageClient.getObject(normalizedObjectName);
            return new StoredFile(storedObject.content(), normalizeContentType(storedObject.contentType()));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
    }

    private String normalizeScene(String scene) {
        String normalized = StringUtils.hasText(scene) ? scene.trim().toLowerCase() : "activity";
        if (!ALLOWED_SCENES.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的上传场景");
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType) || !IMAGE_EXTENSIONS.containsKey(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只支持 JPG、PNG、WebP 图片");
        }
        return contentType;
    }

    private String buildObjectName(String scene, String contentType) {
        LocalDate today = LocalDate.now();
        return "%s/%d/%02d/%s.%s".formatted(
                scene,
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                IMAGE_EXTENSIONS.get(contentType)
        );
    }

    private String detectImageContentType(byte[] content) {
        if (content == null || content.length < 4) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "鍙敮鎸?JPG銆丳NG銆乄ebP 鍥剧墖");
        }
        if (content.length >= 3
                && unsigned(content[0]) == 0xFF
                && unsigned(content[1]) == 0xD8
                && unsigned(content[2]) == 0xFF) {
            return "image/jpeg";
        }
        if (content.length >= 8
                && unsigned(content[0]) == 0x89
                && content[1] == 0x50
                && content[2] == 0x4E
                && content[3] == 0x47
                && content[4] == 0x0D
                && content[5] == 0x0A
                && content[6] == 0x1A
                && content[7] == 0x0A) {
            return "image/png";
        }
        if (content.length >= 12
                && content[0] == 0x52
                && content[1] == 0x49
                && content[2] == 0x46
                && content[3] == 0x46
                && content[8] == 0x57
                && content[9] == 0x45
                && content[10] == 0x42
                && content[11] == 0x50) {
            return "image/webp";
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "鍙敮鎸?JPG銆丳NG銆乄ebP 鍥剧墖");
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private String normalizeObjectName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片路径不能为空");
        }
        String normalized = objectName.startsWith("/") ? objectName.substring(1) : objectName;
        if (normalized.contains("..") || normalized.startsWith("/") || normalized.endsWith("/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片路径不合法");
        }
        return normalized;
    }
}
