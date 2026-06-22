package com.techstore.service;

import com.techstore.exception.BusinessLogicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${app.upload.max-file-size:10485760}") // 10MB
    private long maxFileSize;

    @Value("${app.upload.allowed-extensions:jpg,jpeg,png,gif,webp}")
    private String allowedExtensionsStr;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadProductImage(MultipartFile file, String subfolder) {
        validateFile(file);

        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String key = subfolder + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);

            log.info("Successfully uploaded image to S3: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new BusinessLogicException("Failed to upload file: " + e.getMessage());
        }
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String key = extractKeyFromUrl(imageUrl);

            if (key.isBlank()) {
                log.debug("Skipping S3 delete — URL is not an S3 object: {}", imageUrl);
                return;
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted image from S3: {}", imageUrl);

        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", e.getMessage());
            // Don't throw exception here to avoid breaking the main operation
        }
    }

    public void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (String imageUrl : imageUrls) {
            deleteImage(imageUrl);
        }
    }

    // ============ WARRANTY FILE OPERATIONS ============

    public String uploadWarrantyFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessLogicException("Warranty file cannot be empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new BusinessLogicException("File size exceeds maximum allowed size");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        List<String> allowed = Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "odt", "ods", "rtf", "txt", "jpg", "jpeg", "png", "webp", "heic");
        if (!allowed.contains(extension)) {
            throw new BusinessLogicException("File type not allowed for warranty documents. Allowed: " + allowed);
        }

        String key = "warranties/" + UUID.randomUUID() + "." + extension;
        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Warranty file uploaded to S3: {}", key);
            return key;
        } catch (IOException e) {
            log.error("Error uploading warranty file to S3: {}", e.getMessage());
            throw new BusinessLogicException("Failed to upload warranty file: " + e.getMessage());
        }
    }

    public byte[] downloadFileBytes(String key) {
        try {
            GetObjectRequest get = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(get)) {
                return response.readAllBytes();
            }
        } catch (IOException e) {
            log.error("Error downloading file from S3: {}", key, e);
            throw new BusinessLogicException("Failed to download file from S3: " + e.getMessage());
        }
    }

    public void deleteWarrantyFile(String key) {
        if (key == null || key.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            log.info("Warranty file deleted from S3: {}", key);
        } catch (Exception e) {
            log.error("Error deleting warranty file from S3: {}", key, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessLogicException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessLogicException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (extension == null || extension.isEmpty()) {
            throw new BusinessLogicException("File has no extension");
        }

        extension = extension.toLowerCase();

        // Парсни правилно allowed extensions
        List<String> allowedExtList = Arrays.asList(allowedExtensionsStr.split(","));

        log.info("Validating file '{}' with extension '{}'. Allowed: {}",
                originalFilename, extension, allowedExtList);

        if (!allowedExtList.contains(extension)) {
            throw new BusinessLogicException(
                    String.format("File extension '%s' not allowed. Allowed extensions: %s",
                            extension, allowedExtensionsStr));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String extractKeyFromUrl(String imageUrl) {
        // Extract key from URL like: https://bucket.s3.region.amazonaws.com/key
        // ".amazonaws.com/" is 15 characters — use +15 to skip past the trailing slash
        try {
            int idx = imageUrl.indexOf(".amazonaws.com/");
            if (idx == -1) {
                log.warn("URL is not an S3 URL, cannot extract key: {}", imageUrl);
                return "";
            }
            return imageUrl.substring(idx + 15);
        } catch (Exception e) {
            log.warn("Could not extract key from URL: {}", imageUrl);
            return "";
        }
    }
}