package com.microservices.hrms.dto;

import com.microservices.hrms.entity.OvertimeRequest;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class OvertimeRequestDto {
    
    // Public view for overtime board (visible to everyone)
    @Data
    @Builder
    public static class OvertimeBoard {
        private Long id;
        private String employeeName;
        private String employeeId;
        private Long authUserId;
        private LocalDate overtimeDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal hoursRequested;
        private OvertimeRequest.OvertimeType overtimeType;
        private String overtimeTypeDisplay;
        private String projectOrTask;
        private boolean isToday;
        private boolean isWeekend;
        private boolean isUrgent;
        private String avatarUrl;
    }
    
    // Detailed view for the employee who created the request
    @Data
    @Builder
    public static class UserView {
        private Long id;
        private LocalDate overtimeDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal hoursRequested;
        private OvertimeRequest.OvertimeType overtimeType;
        private String overtimeTypeDisplay;
        private OvertimeRequest.RequestStatus status;
        private String statusDisplay;
        private String reason;
        private String projectOrTask;
        private boolean isUrgent;
        private BigDecimal estimatedRate;
        private BigDecimal estimatedCost;
        private BigDecimal actualHoursWorked;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String approvedByName;
        private LocalDateTime approvedAt;
        private String rejectionReason;
        private boolean isToday;
        private boolean canCancel;
        private boolean canEdit;
    }
    
    // Management view for HR/managers (includes approval actions and cost info)
    @Data
    @Builder
    public static class ManagementView {
        private Long id;
        private String employeeName;
        private String employeeId;
        private Long authUserId;
        private String employeeEmail;
        private String department;
        private LocalDate overtimeDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal hoursRequested;
        private OvertimeRequest.OvertimeType overtimeType;
        private String overtimeTypeDisplay;
        private OvertimeRequest.RequestStatus status;
        private String statusDisplay;
        private String reason;
        private String projectOrTask;
        private boolean isUrgent;
        private BigDecimal estimatedRate;
        private BigDecimal estimatedCost;
        private BigDecimal actualHoursWorked;
        private LocalDateTime createdAt;
        private String approvedByName;
        private LocalDateTime approvedAt;
        private String rejectionReason;
        private boolean isToday;
        private boolean canApprove;
        private boolean canReject;
        private String avatarUrl;
        
        // Additional info for managers
        private BigDecimal totalOvertimeHoursThisYear;
        private BigDecimal totalOvertimeHoursThisMonth;
        private BigDecimal estimatedMonthlyCost;
        private boolean hasOverlappingRequests;
    }
    
    // Request creation DTO
    @Data
    @Builder
    public static class CreateRequest {
        private LocalDate overtimeDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal hoursRequested;
        private OvertimeRequest.OvertimeType overtimeType;
        private String reason;
        private String projectOrTask;
        private boolean isUrgent;
    }
    
    // Request update DTO (for editing pending requests)
    @Data
    @Builder
    public static class UpdateRequest {
        private LocalDate overtimeDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal hoursRequested;
        private OvertimeRequest.OvertimeType overtimeType;
        private String reason;
        private String projectOrTask;
        private boolean isUrgent;
    }
    
    // Approval/Rejection DTO
    @Data
    @Builder
    public static class ApprovalAction {
        private OvertimeRequest.RequestStatus action; // APPROVED or REJECTED
        private String reason; // Optional reason for rejection
        private BigDecimal estimatedRate; // Set rate when approving
    }
    
    // Completion DTO (when work is done)
    @Data
    @Builder
    public static class CompletionAction {
        private BigDecimal actualHoursWorked;
        private String notes;
    }
    
    // Summary statistics DTO
    @Data
    @Builder
    public static class Statistics {
        private Long totalRequests;
        private Long pendingRequests;
        private Long approvedRequests;
        private Long rejectedRequests;
        private Long urgentRequests;
        private Long todaysOvertime;
        private Long upcomingOvertime;
        private BigDecimal totalHoursThisYear;
        private BigDecimal totalHoursThisMonth;
        private BigDecimal totalEstimatedCostThisMonth;
        private BigDecimal averageHoursPerRequest;
    }
    
    // Overtime board summary for dashboard
    @Data
    @Builder
    public static class OvertimeBoardSummary {
        private Integer todaysOvertime;
        private Integer tomorrowsOvertime;
        private Integer thisWeeksOvertime;
        private Integer pendingApproval;
        private Integer urgentRequests;
        private BigDecimal thisWeeksHours;
        private BigDecimal thisMonthsHours;
    }
    
    // Cost analysis DTO
    @Data
    @Builder
    public static class CostAnalysis {
        private BigDecimal totalEstimatedCost;
        private BigDecimal totalActualCost;
        private BigDecimal averageCostPerHour;
        private BigDecimal monthlyCostTrend;
        private BigDecimal weekendOvertimeCost;
        private BigDecimal emergencyOvertimeCost;
    }
} 