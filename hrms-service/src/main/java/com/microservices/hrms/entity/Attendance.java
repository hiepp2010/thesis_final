package com.microservices.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "attendance_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;
    
    @Column(name = "check_in_time")
    private LocalTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalTime checkOutTime;
    
    @Column(name = "break_start_time")
    private LocalDateTime breakStartTime;
    
    @Column(name = "break_end_time")
    private LocalDateTime breakEndTime;
    
    @Column(name = "total_hours", precision = 4, scale = 2)
    private BigDecimal totalHours;
    
    @Column(name = "regular_hours", precision = 4, scale = 2)
    private BigDecimal regularHours;
    
    @Column(name = "overtime_hours", precision = 4, scale = 2)
    private BigDecimal overtimeHours;
    
    @Column(name = "break_duration_minutes")
    @Builder.Default
    private Integer breakDurationMinutes = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status;
    
    @Column(name = "location", length = 100)
    private String location;
    
    @Column(name = "ip_address")
    private InetAddress ipAddress;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "is_modified", nullable = false)
    @Builder.Default
    private Boolean isModified = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private Employee modifiedBy;
    
    @Column(name = "modification_reason", length = 500)
    private String modificationReason;
    
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus;
    
    @Column(name = "original_check_in_time")
    private LocalTime originalCheckInTime;
    
    @Column(name = "original_check_out_time")
    private LocalTime originalCheckOutTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "original_status")
    private AttendanceStatus originalStatus;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AttendanceStatus.PENDING;
        }
        if (approvalStatus == null) {
            approvalStatus = ApprovalStatus.NOT_REQUIRED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum AttendanceStatus {
        PRESENT("Present"),
        ABSENT("Absent"),
        LATE("Late"),
        EARLY_DEPARTURE("Early Departure"),
        HALF_DAY("Half Day"),
        SICK_LEAVE("Sick Leave"),
        VACATION("Vacation"),
        HOLIDAY("Holiday"),
        REMOTE_WORK("Remote Work"),
        BUSINESS_TRIP("Business Trip"),
        PENDING("Pending");
        
        private final String displayName;
        
        AttendanceStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ApprovalStatus {
        NOT_REQUIRED("Not Required"),
        PENDING("Pending Approval"),
        APPROVED("Approved"),
        REJECTED("Rejected");
        
        private final String displayName;
        
        ApprovalStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public boolean isPresent() {
        return status == AttendanceStatus.PRESENT || 
               status == AttendanceStatus.LATE || 
               status == AttendanceStatus.EARLY_DEPARTURE ||
               status == AttendanceStatus.REMOTE_WORK;
    }
    
    public boolean isCheckedIn() {
        return checkInTime != null;
    }
    
    public boolean isCheckedOut() {
        return checkOutTime != null;
    }
    
    public boolean isWorkingDay() {
        return status != AttendanceStatus.ABSENT && 
               status != AttendanceStatus.HOLIDAY && 
               status != AttendanceStatus.VACATION &&
               status != AttendanceStatus.SICK_LEAVE;
    }
    
    public boolean needsApproval() {
        return isModified && approvalStatus == ApprovalStatus.PENDING;
    }
    
    public boolean isLate() {
        LocalTime standardStartTime = LocalTime.of(9, 0);
        return checkInTime != null && checkInTime.isAfter(standardStartTime);
    }
    
    public boolean isEarlyDeparture() {
        LocalTime standardEndTime = LocalTime.of(18, 0);
        return checkOutTime != null && checkOutTime.isBefore(standardEndTime);
    }
    
    public BigDecimal calculateWorkingHours() {
        if (checkInTime == null || checkOutTime == null) {
            return BigDecimal.ZERO;
        }
        
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(checkInTime, checkOutTime);
        if (breakDurationMinutes != null) {
            minutes -= breakDurationMinutes;
        }
        
        return new BigDecimal(minutes).divide(new BigDecimal(60), 2, java.math.RoundingMode.HALF_UP);
    }
} 