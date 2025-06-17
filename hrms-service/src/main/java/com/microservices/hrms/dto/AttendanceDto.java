package com.microservices.hrms.dto;

import com.microservices.hrms.entity.Attendance;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AttendanceDto {
    
    // Public view for attendance board (visible to everyone)
    @Data
    @Builder
    public static class AttendanceBoard {
        private Long id;
        private String employeeName;
        private String employeeId;
        private Long authUserId;
        private LocalDate attendanceDate;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Attendance.AttendanceStatus status;
        private String statusDisplay;
        private String location;
        private BigDecimal totalHoursWorked;
        private boolean isPresent;
        private boolean isLate;
        private boolean isStillWorking;
        private String avatarUrl;
        private String department;
    }
    
    // User view for their own attendance
    @Data
    @Builder
    public static class UserView {
        private Long id;
        private LocalDate attendanceDate;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Attendance.AttendanceStatus status;
        private String statusDisplay;
        private String location;
        private String notes;
        private BigDecimal totalHoursWorked;
        private BigDecimal overtimeHours;
        private Integer breakTimeMinutes;
        private boolean isPresent;
        private boolean isLate;
        private boolean isEarlyDeparture;
        private boolean canCheckIn;
        private boolean canCheckOut;
        private boolean canModify;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        // Modification tracking
        private boolean isModified;
        private String modificationReason;
        private String modifiedByName;
        private LocalDateTime modifiedAt;
        private Attendance.ApprovalStatus approvalStatus;
        private String approvalStatusDisplay;
        private String approvedByName;
        private LocalDateTime approvedAt;
        private String approvalNotes;
        
        // Original values (if modified)
        private LocalTime originalCheckInTime;
        private LocalTime originalCheckOutTime;
        private Attendance.AttendanceStatus originalStatus;
    }
    
    // HR view with modification capabilities
    @Data
    @Builder
    public static class HRView {
        private Long id;
        private String employeeName;
        private String employeeId;
        private Long authUserId;
        private String employeeEmail;
        private String department;
        private LocalDate attendanceDate;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Attendance.AttendanceStatus status;
        private String statusDisplay;
        private String location;
        private String notes;
        private BigDecimal totalHoursWorked;
        private BigDecimal overtimeHours;
        private Integer breakTimeMinutes;
        private boolean isPresent;
        private boolean isLate;
        private boolean isEarlyDeparture;
        private boolean canModify;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String avatarUrl;
        
        // Modification tracking
        private boolean isModified;
        private String modificationReason;
        private String modifiedByName;
        private LocalDateTime modifiedAt;
        private Attendance.ApprovalStatus approvalStatus;
        private String approvalStatusDisplay;
        private String approvedByName;
        private LocalDateTime approvedAt;
        private String approvalNotes;
        
        // Original values (if modified)
        private LocalTime originalCheckInTime;
        private LocalTime originalCheckOutTime;
        private Attendance.AttendanceStatus originalStatus;
        
        // Employee statistics for context
        private Long totalPresentDaysThisMonth;
        private Long totalAbsentDaysThisMonth;
        private Long totalLateDaysThisMonth;
        private BigDecimal totalHoursThisMonth;
    }
    
    // Manager view for approvals
    @Data
    @Builder
    public static class ManagerView {
        private Long id;
        private String employeeName;
        private String employeeId;
        private Long authUserId;
        private String employeeEmail;
        private String department;
        private LocalDate attendanceDate;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Attendance.AttendanceStatus status;
        private String statusDisplay;
        private String location;
        private String notes;
        private BigDecimal totalHoursWorked;
        private BigDecimal overtimeHours;
        private Integer breakTimeMinutes;
        private LocalDateTime createdAt;
        private String avatarUrl;
        
        // Modification details
        private boolean isModified;
        private String modificationReason;
        private String modifiedByName;
        private LocalDateTime modifiedAt;
        private Attendance.ApprovalStatus approvalStatus;
        private String approvalStatusDisplay;
        private boolean canApprove;
        private boolean canReject;
        
        // Original vs Modified comparison
        private LocalTime originalCheckInTime;
        private LocalTime originalCheckOutTime;
        private Attendance.AttendanceStatus originalStatus;
        private String originalStatusDisplay;
        
        // Employee context
        private Long totalPresentDaysThisMonth;
        private Long totalModificationsThisMonth;
        private BigDecimal attendanceRate;
    }
    
    // Check-in request DTO
    @Data
    @Builder
    public static class CheckInRequest {
        private String location;
        private String notes;
    }
    
    // Check-out request DTO
    @Data
    @Builder
    public static class CheckOutRequest {
        private Integer breakTimeMinutes;
        private String notes;
    }
    
    // HR modification request DTO
    @Data
    @Builder
    public static class ModificationRequest {
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Attendance.AttendanceStatus status;
        private String location;
        private String notes;
        private Integer breakTimeMinutes;
        private BigDecimal overtimeHours;
        private String modificationReason;
    }
    
    // Manager approval DTO
    @Data
    @Builder
    public static class ApprovalAction {
        private Attendance.ApprovalStatus action; // APPROVED or REJECTED
        private String approvalNotes;
    }
    
    // Attendance statistics DTO
    @Data
    @Builder
    public static class Statistics {
        private Long totalDays;
        private Long presentDays;
        private Long absentDays;
        private Long lateDays;
        private Long earlyDepartureDays;
        private Long remoteDays;
        private BigDecimal attendanceRate;
        private BigDecimal punctualityRate;
        private BigDecimal totalHoursWorked;
        private BigDecimal averageHoursPerDay;
        private BigDecimal totalOvertimeHours;
        private Long modificationsCount;
        private Long pendingApprovalsCount;
    }
    
    // Daily attendance summary
    @Data
    @Builder
    public static class DailySummary {
        private LocalDate date;
        private Integer totalEmployees;
        private Integer presentEmployees;
        private Integer absentEmployees;
        private Integer remoteEmployees;
        private Integer lateEmployees;
        private Integer stillWorkingEmployees;
        private BigDecimal averageWorkingHours;
        private BigDecimal attendancePercentage;
    }
    
    // Attendance board summary for dashboard
    @Data
    @Builder
    public static class AttendanceBoardSummary {
        private Integer presentToday;
        private Integer absentToday;
        private Integer remoteToday;
        private Integer lateToday;
        private Integer stillWorking;
        private Integer pendingApprovals;
        private Integer modificationsToday;
        private BigDecimal todayAttendanceRate;
        private BigDecimal weekAttendanceRate;
        private BigDecimal monthAttendanceRate;
    }
    
    // Attendance report DTO
    @Data
    @Builder
    public static class AttendanceReport {
        private String employeeName;
        private String employeeId;
        private String department;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long totalWorkingDays;
        private Long presentDays;
        private Long absentDays;
        private Long lateDays;
        private Long remoteDays;
        private BigDecimal totalHoursWorked;
        private BigDecimal averageHoursPerDay;
        private BigDecimal attendanceRate;
        private BigDecimal punctualityRate;
        private String notes;
    }
} 