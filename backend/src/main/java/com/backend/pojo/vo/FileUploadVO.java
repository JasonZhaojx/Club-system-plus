package com.backend.pojo.vo;

public record FileUploadVO(
        String url,
        String objectName,
        String contentType,
        long size
) {
}
