package com.techstore.controller;

import com.techstore.service.FileUploadService;
import com.techstore.service.S3Service;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Hidden
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final S3Service s3Service;

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        log.info("Uploading product image: {}", file.getOriginalFilename());
        String filePath = fileUploadService.uploadFile(file, "products");
        return ResponseEntity.ok(Map.of("url", filePath));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadCategoryImage(@RequestParam("file") MultipartFile file) {
        log.info("Uploading category image: {}", file.getOriginalFilename());
        String filePath = fileUploadService.uploadFile(file, "categories");
        return ResponseEntity.ok(Map.of("url", filePath));
    }

    @PostMapping("/brands")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadBrandLogo(@RequestParam("file") MultipartFile file) {
        log.info("Uploading brand logo: {}", file.getOriginalFilename());
        String filePath = fileUploadService.uploadFile(file, "brands");
        return ResponseEntity.ok(Map.of("url", filePath));
    }

    @PostMapping("/blog-cover")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadBlogCover(@RequestParam("file") MultipartFile file) {
        log.info("Uploading blog cover image: {}", file.getOriginalFilename());
        String url = s3Service.uploadProductImage(file, "blog-covers");
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/blog-image")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadBlogImage(@RequestParam("file") MultipartFile file) {
        log.info("Uploading blog inline image: {}", file.getOriginalFilename());
        String url = s3Service.uploadProductImage(file, "blog-images");
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> deleteFile(@RequestParam("path") String filePath) {
        log.info("Deleting file: {}", filePath);
        fileUploadService.deleteFile(filePath);
        return ResponseEntity.ok("File deleted successfully");
    }
}