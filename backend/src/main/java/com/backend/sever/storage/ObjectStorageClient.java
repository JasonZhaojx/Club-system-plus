package com.backend.sever.storage;

import java.io.InputStream;

public interface ObjectStorageClient {
    void putObject(String objectName, InputStream inputStream, long size, String contentType);

    StoredObject getObject(String objectName);

    record StoredObject(byte[] content, String contentType) {
    }
}
