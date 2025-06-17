package com.microservices.chat.dto;

import com.microservices.chat.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    
    private Long id;
    
    @NotNull(message = "Chat room ID is required")
    private Long chatRoomId;
    
    private String chatRoomName;
    
    private Long senderId;
    
    private String senderUsername;
    
    private String senderFullName;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    private Message.MessageType messageType = Message.MessageType.TEXT;
    
    private Long replyToId;
    
    private String attachmentUrl;
    
    private String attachmentName;
    
    private Long attachmentSize;
    
    private Boolean isEdited = false;
    
    private Boolean isDeleted = false;
    
    private LocalDateTime editedAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
} 