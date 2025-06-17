package com.microservices.hrms.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "auth_user_id", unique = true, nullable = false)
    @NotNull(message = "Auth user ID is required")
    private Long authUserId;
    
    @Column(name = "employee_id", unique = true, nullable = false, length = 20)
    @NotBlank(message = "Employee ID is required")
    private String employeeId;
    
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Name is required")
    private String name;
    
    @Column(name = "email", unique = true, nullable = false, length = 100)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonBackReference("department-employees")
    private Department department;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_position_id")
    private JobPosition jobPosition;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;
    
    @Column(name = "hire_date", nullable = false)
    @NotNull(message = "Hire date is required")
    private LocalDate hireDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    @Builder.Default
    private EmploymentType employmentType = EmploymentType.FULL_TIME;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "work_location", length = 20)
    @Builder.Default
    private WorkLocation workLocation = WorkLocation.OFFICE;
    
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;
    
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills", columnDefinition = "jsonb")
    private List<String> skills;
    
    @JsonIgnore
    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;
    
    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "USD";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
    
    @Column(name = "termination_date")
    private LocalDate terminationDate;
    
    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EmployeePersonalInfo personalInfo;
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attendance> attendanceRecords;
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LeaveRequest> leaveRequests;
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AssetAssignment> assetAssignments;
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payslip> payslips;
    
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
    
    // Utility methods
    public String getFullName() {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }
        return name;
    }
    
    public boolean isActive() {
        return status == EmployeeStatus.ACTIVE;
    }
    
    // Enums
    public enum EmploymentType {
        FULL_TIME, PART_TIME, CONTRACT, INTERN
    }
    
    public enum WorkLocation {
        OFFICE, REMOTE, HYBRID
    }
    
    public enum EmployeeStatus {
        ACTIVE, INACTIVE, TERMINATED, ON_LEAVE
    }
} 