package com.microservices.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "employee_personal_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePersonalInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "gender", length = 20)
    private String gender;
    
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;
    
    @Column(name = "nationality", length = 50)
    private String nationality;
    
    @Column(name = "personal_email", length = 100)
    private String personalEmail;
    
    @Column(name = "personal_phone", length = 20)
    private String personalPhone;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_address", columnDefinition = "jsonb")
    private Map<String, String> currentAddress;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permanent_address", columnDefinition = "jsonb")
    private Map<String, String> permanentAddress;
    
    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;
    
    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;
    
    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;
    
    @Column(name = "emergency_contact_email", length = 100)
    private String emergencyContactEmail;
    
    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;
    
    @Column(name = "bank_name", length = 100)
    private String bankName;
    
    @Column(name = "bank_branch", length = 100)
    private String bankBranch;
    
    @Column(name = "routing_number", length = 20)
    private String routingNumber;
    
    @Column(name = "tax_id", length = 20)
    private String taxId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 