package com.microservices.ticketservice.service;

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
    private static final String TICKET_ATTACHMENTS_BUCKET = "ticket-attachments";
    private static final String PROJECT_ATTACHMENTS_BUCKET = "project-attachments";
    
    @Autowired
    private MinioClient minioClient;
    
    public void initBuckets() {
        try {
            createBucketIfNotExists(TICKET_ATTACHMENTS_BUCKET);
            createBucketIfNotExists(PROJECT_ATTACHMENTS_BUCKET);
            logger.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing MinIO buckets", e);
        }
    }
    
    private void createBucketIfNotExists(String bucketName) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Created bucket: {}", bucketName);
        }
    }
    
    /**
     * Upload attachment for a ticket
     */
    public String uploadTicketAttachment(Long ticketId, MultipartFile file) throws Exception {
        validateFile(file);
        
        String fileName = generateTicketAttachmentFileName(ticketId, file.getOriginalFilename());
        String contentType = file.getContentType();
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(TICKET_ATTACHMENTS_BUCKET)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
        }
        
        logger.info("Uploaded attachment for ticket {}: {}", ticketId, fileName);
        return fileName;
    }
    
    /**
     * Upload attachment for a project
     */
    public String uploadProjectAttachment(Long projectId, MultipartFile file) throws Exception {
        validateFile(file);
        
        String fileName = generateProjectAttachmentFileName(projectId, file.getOriginalFilename());
        String contentType = file.getContentType();
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(PROJECT_ATTACHMENTS_BUCKET)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
        }
        
        logger.info("Uploaded attachment for project {}: {}", projectId, fileName);
        return fileName;
    }
    
    /**
     * Get ticket attachment URL
     */
    public String getTicketAttachmentUrl(String fileName) throws Exception {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(TICKET_ATTACHMENTS_BUCKET)
                .object(fileName)
                .expiry(1, TimeUnit.HOURS) // URL expires in 1 hour
                .build()
        );
    }
    
    /**
     * Get project attachment URL
     */
    public String getProjectAttachmentUrl(String fileName) throws Exception {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(PROJECT_ATTACHMENTS_BUCKET)
                .object(fileName)
                .expiry(1, TimeUnit.HOURS) // URL expires in 1 hour
                .build()
        );
    }
    
    /**
     * Delete ticket attachment
     */
    public void deleteTicketAttachment(String fileName) throws Exception {
        if (fileName != null && !fileName.isEmpty()) {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(TICKET_ATTACHMENTS_BUCKET)
                    .object(fileName)
                    .build()
            );
            logger.info("Deleted ticket attachment: {}", fileName);
        }
    }
    
    /**
     * Delete project attachment
     */
    public void deleteProjectAttachment(String fileName) throws Exception {
        if (fileName != null && !fileName.isEmpty()) {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(PROJECT_ATTACHMENTS_BUCKET)
                    .object(fileName)
                    .build()
            );
            logger.info("Deleted project attachment: {}", fileName);
        }
    }
    
    private String generateTicketAttachmentFileName(Long ticketId, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return String.format("ticket_%d_%s.%s", ticketId, UUID.randomUUID().toString().substring(0, 8), extension);
    }
    
    private String generateProjectAttachmentFileName(Long projectId, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return String.format("project_%d_%s.%s", projectId, UUID.randomUUID().toString().substring(0, 8), extension);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private void validateFile(MultipartFile file) throws IllegalArgumentException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Check file size (max 20MB for attachments)
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 20MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Unknown file type");
        }
        
        // Allow common file types for attachments
        if (!contentType.equals("application/pdf") && 
            !contentType.startsWith("image/") && 
            !contentType.equals("application/msword") &&
            !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") &&
            !contentType.equals("text/plain") &&
            !contentType.equals("application/zip") &&
            !contentType.equals("application/x-zip-compressed")) {
            throw new IllegalArgumentException("File type not supported. Allowed: PDF, images, Word documents, text files, zip files");
        }
    }
} 