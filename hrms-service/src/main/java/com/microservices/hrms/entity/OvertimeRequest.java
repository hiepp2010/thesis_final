package com.microservices.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "overtime_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @Column(name = "overtime_date", nullable = false)
    private LocalDate overtimeDate;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "hours_requested", nullable = false, precision = 4, scale = 2)
    private BigDecimal hoursRequested;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "overtime_type", nullable = false)
    private OvertimeType overtimeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "project_or_task", length = 255)
    private String projectOrTask;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    @Column(name = "is_urgent", nullable = false)
    @Builder.Default
    private Boolean isUrgent = false;
    
    @Column(name = "estimated_rate", precision = 10, scale = 2)
    private BigDecimal estimatedRate;
    
    @Column(name = "actual_hours_worked", precision = 4, scale = 2)
    private BigDecimal actualHoursWorked;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Enums
    public enum OvertimeType {
        REGULAR("Regular Overtime"),
        WEEKEND("Weekend Work"),
        HOLIDAY("Holiday Work"),
        EMERGENCY("Emergency Overtime"),
        PROJECT_DEADLINE("Project Deadline"),
        MAINTENANCE("System Maintenance"),
        TRAINING("Training/Development"),
        CLIENT_REQUEST("Client Request"),
        OTHER("Other");
        
        private final String displayName;
        
        OvertimeType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum RequestStatus {
        PENDING("Pending"),
        APPROVED("Approved"),
        REJECTED("Rejected"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        RequestStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public boolean isApproved() {
        return status == RequestStatus.APPROVED;
    }
    
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }
    
    public boolean isActive() {
        return isApproved() && !LocalDate.now().isAfter(overtimeDate);
    }
    
    public boolean isToday() {
        return overtimeDate.equals(LocalDate.now());
    }
    
    public boolean isFutureWork() {
        return overtimeDate.isAfter(LocalDate.now());
    }
    
    public boolean isWeekend() {
        return overtimeDate.getDayOfWeek().getValue() >= 6; // Saturday or Sunday
    }
    
    public BigDecimal getEstimatedCost() {
        if (estimatedRate != null && hoursRequested != null) {
            return estimatedRate.multiply(hoursRequested);
        }
        return BigDecimal.ZERO;
    }
} 