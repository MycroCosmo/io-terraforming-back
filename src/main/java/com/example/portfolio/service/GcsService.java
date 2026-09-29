package com.example.portfolio.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.portfolio.exception.CustomException;
import com.example.portfolio.exception.ErrorCode;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

@Service
public class GcsService {

    private static final Logger log = LoggerFactory.getLogger(GcsService.class);

    private final String bucketName;
    private final Storage storage;

    public GcsService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                      @Value("${spring.cloud.gcp.storage.bucket}") String bucketName) throws IOException {
        this(createStorage(keyFileName), bucketName);
    }

    GcsService(Storage storage, String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    private static Storage createStorage(String keyFileName) throws IOException {
        try (InputStream keyFile = ResourceUtils.getURL(keyFileName).openStream()) {
            return StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(keyFile))
                    .build()
                    .getService();
        }
    }

    // WebP 파일 업로드 메서드
    public String uploadWebpFile(MultipartFile multipartFile, Long projectId) {
        try {
            String uuid = UUID.randomUUID().toString();
            String objectName = projectId + "/" + uuid + ".webp";

            // WebP 이미지 변환
            ImmutableImage image = ImmutableImage.loader().fromStream(multipartFile.getInputStream());
            WebpWriter writer = WebpWriter.DEFAULT.withQ(80).withM(4).withZ(9);
            byte[] webpBytes = image.bytes(writer);

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType("image/webp")
                    .build();

            storage.create(blobInfo, webpBytes);

            return "https://storage.googleapis.com/" + bucketName + "/" + objectName;

        } catch (IOException | RuntimeException e) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.STORAGE_IO_ERROR,
                    "Failed to upload file: " + e.getMessage()
            );
        }
    }

    public void deleteAfterCommit(String url) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteFile(url);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(url);
            }
        });
    }

    public void deleteOnRollback(String url) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteQuietly(url);
            }
        });
    }

    private void deleteQuietly(String url) {
        try {
            deleteFile(url);
        } catch (RuntimeException e) {
            log.error("Failed to clean up storage object: {}", url, e);
        }
    }

    private void deleteFile(String url) {
        if (url == null || url.isBlank()) return;
        storage.delete(bucketName, getObjectNameFromUrl(url));
    }

    // url로 object 이름 가져오는 메서드
    public String getObjectNameFromUrl(String url) {
        String prefix = "https://storage.googleapis.com/" + bucketName + "/";
        if (url == null || !url.startsWith(prefix) || url.length() == prefix.length()) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.STORAGE_IO_ERROR,
                    "Invalid storage URL"
            );
        }
        return url.substring(prefix.length());
    }
}
