package com.microservices.hrms.service;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final String AVATAR_BUCKET = "avatars";
    private static final String DOCUMENT_BUCKET = "documents";
    
    @Autowired
    private MinioClient minioClient;
    
    public void initBuckets() {
        try {
            createBucketIfNotExists(AVATAR_BUCKET);
            createBucketIfNotExists(DOCUMENT_BUCKET);
            logger.info("Minio buckets initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing Minio buckets", e);
        }
    }
    
    private void createBucketIfNotExists(String bucketName) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Created bucket: {}", bucketName);
        }
    }
    
    /**
     * Upload avatar for an employee
     */
    public String uploadAvatar(Long employeeId, MultipartFile file) throws Exception {
        validateImageFile(file);
        
        String fileName = generateAvatarFileName(employeeId, file.getOriginalFilename());
        String contentType = file.getContentType();
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(AVATAR_BUCKET)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
        }
        
        logger.info("Uploaded avatar for employee {}: {}", employeeId, fileName);
        return fileName;
    }
    
    /**
     * Get avatar URL for an employee
     */
    public String getAvatarUrl(String fileName) throws Exception {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(AVATAR_BUCKET)
                .object(fileName)
                .expiry(7, TimeUnit.DAYS) // URL expires in 7 days
                .build()
        );
    }
    
    /**
     * Delete avatar file
     */
    public void deleteAvatar(String fileName) throws Exception {
        if (fileName != null && !fileName.isEmpty()) {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(AVATAR_BUCKET)
                    .object(fileName)
                    .build()
            );
            logger.info("Deleted avatar: {}", fileName);
        }
    }
    
    /**
     * Upload document (resume, certificates, etc.)
     */
    public String uploadDocument(Long employeeId, MultipartFile file, String documentType) throws Exception {
        validateDocumentFile(file);
        
        String fileName = generateDocumentFileName(employeeId, documentType, file.getOriginalFilename());
        String contentType = file.getContentType();
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(DOCUMENT_BUCKET)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
        }
        
        logger.info("Uploaded document for employee {}: {}", employeeId, fileName);
        return fileName;
    }
    
    /**
     * Get document URL
     */
    public String getDocumentUrl(String fileName) throws Exception {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(DOCUMENT_BUCKET)
                .object(fileName)
                .expiry(1, TimeUnit.HOURS) // Shorter expiry for documents
                .build()
        );
    }
    
    private String generateAvatarFileName(Long employeeId, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return String.format("employee_%d_%s.%s", employeeId, UUID.randomUUID().toString().substring(0, 8), extension);
    }
    
    private String generateDocumentFileName(Long employeeId, String documentType, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return String.format("employee_%d_%s_%s.%s", employeeId, documentType, UUID.randomUUID().toString().substring(0, 8), extension);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private void validateImageFile(MultipartFile file) throws IllegalArgumentException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
        
        // Check file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }
    }
    
    private void validateDocumentFile(MultipartFile file) throws IllegalArgumentException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Check file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Unknown file type");
        }
        
        // Allow common document types
        if (!contentType.equals("application/pdf") && 
            !contentType.startsWith("image/") && 
            !contentType.equals("application/msword") &&
            !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw new IllegalArgumentException("File type not supported. Allowed: PDF, images, Word documents");
        }
    }
} 