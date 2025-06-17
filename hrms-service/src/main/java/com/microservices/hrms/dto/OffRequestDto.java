package com.microservices.hrms.dto;

import com.microservices.hrms.entity.OffRequest;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OffRequestDto {
    
    // Public view for time board (visible to everyone)
    @Data
    @Builder
    public static class TimeBoard {
        private Long id;
        private String employeeName;
        private String employeeId;
        private Long authUserId;
        private LocalDate startDate;
        private LocalDate endDate;
        private OffRequest.LeaveType leaveType;
        private String leaveTypeDisplay;
        private Integer daysRequested;
        private boolean isCurrentlyOnLeave;
        private boolean isEmergency;
        private String avatarUrl;
    }
    
    // Detailed view for the employee who created the request
    @Data
    @Builder
    public static class UserView {
        private Long id;
        private LocalDate startDate;
        private LocalDate endDate;
        private OffRequest.LeaveType leaveType;
        private String leaveTypeDisplay;
        private OffRequest.RequestStatus status;
        private String statusDisplay;
        private String reason;
        private Integer daysRequested;
        private boolean isEmergency;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String approvedByName;
        private LocalDateTime approvedAt;
        private String rejectionReason;
        private boolean isCurrentlyOnLeave;
        private boolean canCancel;
        private boolean canEdit;
    }
    
    // Management view for HR/managers (includes approval actions)
    @Data
    @Builder
    public static class ManagementView {
        private Long id;
        private String employeeName;
        private String employeeId;
        private Long authUserId;
        private String employeeEmail;
        private String department;
        private LocalDate startDate;
        private LocalDate endDate;
        private OffRequest.LeaveType leaveType;
        private String leaveTypeDisplay;
        private OffRequest.RequestStatus status;
        private String statusDisplay;
        private String reason;
        private Integer daysRequested;
        private boolean isEmergency;
        private LocalDateTime createdAt;
        private String approvedByName;
        private LocalDateTime approvedAt;
        private String rejectionReason;
        private boolean isCurrentlyOnLeave;
        private boolean canApprove;
        private boolean canReject;
        private String avatarUrl;
        
        // Additional info for managers
        private Long remainingDaysThisYear;
        private Long usedDaysThisYear;
        private boolean hasOverlappingRequests;
    }
    
    // Request creation DTO
    @Data
    @Builder
    public static class CreateRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private OffRequest.LeaveType leaveType;
        private String reason;
        private boolean isEmergency;
    }
    
    // Request update DTO (for editing pending requests)
    @Data
    @Builder
    public static class UpdateRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private OffRequest.LeaveType leaveType;
        private String reason;
        private boolean isEmergency;
    }
    
    // Approval/Rejection DTO
    @Data
    @Builder
    public static class ApprovalAction {
        private OffRequest.RequestStatus action; // APPROVED or REJECTED
        private String reason; // Optional reason for rejection
    }
    
    // Summary statistics DTO
    @Data
    @Builder
    public static class Statistics {
        private Long totalRequests;
        private Long pendingRequests;
        private Long approvedRequests;
        private Long rejectedRequests;
        private Long currentlyOnLeave;
        private Long upcomingLeave;
        private Long totalDaysUsedThisYear;
        private Long totalDaysRequestedThisYear;
    }
    
    // Time board summary for dashboard
    @Data
    @Builder
    public static class TimeBoardSummary {
        private Integer currentlyOnLeave;
        private Integer upcomingLeaveThisWeek;
        private Integer upcomingLeaveThisMonth;
        private Integer pendingApproval;
    }
} 