package com.backend.sever.service;

import com.backend.pojo.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileUploadVO uploadImage(MultipartFile file, String scene);

    StoredFile getImage(String objectName);

    record StoredFile(byte[] content, String contentType) {
    }
}
