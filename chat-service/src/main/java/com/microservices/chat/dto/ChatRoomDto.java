package com.microservices.chat.dto;

import com.microservices.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    
    private Long id;
    
    @NotBlank(message = "Room name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Room type is required")
    private ChatRoom.RoomType roomType;
    
    private Long createdById;
    
    private String createdByUsername;
    
    private Boolean isPrivate = false;
    
    private Integer maxMembers;
    
    private Integer currentMemberCount;
    
    private List<ChatRoomMemberDto> members;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
} 