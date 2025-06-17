package com.microservices.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.LocalDateTime;

@Document(indexName = "chat-messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDocument {
    
    @Id
    private String id; // Will be messageId from MySQL
    
    @Field(type = FieldType.Long)
    private Long messageId;
    
    @Field(type = FieldType.Long)
    private Long chatRoomId;
    
    @Field(type = FieldType.Text)
    private String chatRoomName;
    
    @Field(type = FieldType.Long)
    private Long senderId;
    
    @Field(type = FieldType.Text)
    private String senderUsername;
    
    @Field(type = FieldType.Text)
    private String senderFullName;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;
    
    @Field(type = FieldType.Keyword)
    private String messageType;
    
    @Field(type = FieldType.Long)
    private Long replyToId;
    
    @Field(type = FieldType.Text)
    private String attachmentUrl;
    
    @Field(type = FieldType.Text)
    private String attachmentName;
    
    @Field(type = FieldType.Long)
    private Long attachmentSize;
    
    @Field(type = FieldType.Boolean)
    private Boolean isEdited;
    
    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime indexedAt;
} 