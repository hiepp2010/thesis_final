package com.microservices.chat.dto;

import com.microservices.chat.entity.ChatRoomMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMemberDto {
    
    private Long id;
    
    private Long chatRoomId;
    
    private Long userId;
    
    private String username;
    
    private String fullName;
    
    private String profilePictureUrl;
    
    private ChatRoomMember.MemberRole role;
    
    private LocalDateTime joinedAt;
    
    private LocalDateTime lastReadAt;
    
    private Boolean isMuted;
    
    private Boolean isActive;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
} 