package com.microservices.hrms.service;

import com.microservices.hrms.dto.OffRequestDto;
import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.entity.OffRequest;
import com.microservices.hrms.repository.EmployeeRepository;
import com.microservices.hrms.repository.OffRequestRepository;
import com.microservices.hrms.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OffRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(OffRequestService.class);
    
    @Autowired
    private OffRequestRepository offRequestRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Get time board - approved requests visible to everyone
     */
    public List<OffRequestDto.TimeBoard> getTimeBoard(LocalDate startDate, LocalDate endDate) {
        List<OffRequest> requests;
        
        if (startDate != null && endDate != null) {
            requests = offRequestRepository.findApprovedRequestsInDateRange(startDate, endDate);
        } else {
            // Default to show current month + 3 months ahead
            LocalDate defaultStart = LocalDate.now().withDayOfMonth(1);
            LocalDate defaultEnd = defaultStart.plusMonths(3).withDayOfMonth(defaultStart.plusMonths(3).lengthOfMonth());
            requests = offRequestRepository.findApprovedRequestsInDateRange(defaultStart, defaultEnd);
        }
        
        return requests.stream()
                .map(this::convertToTimeBoardDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get time board summary for dashboard
     */
    public OffRequestDto.TimeBoardSummary getTimeBoardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusWeeks(1);
        LocalDate monthEnd = today.plusMonths(1);
        
        List<OffRequest> currentlyOnLeave = offRequestRepository.findCurrentActiveLeave(today);
        List<OffRequest> upcomingThisWeek = offRequestRepository.findApprovedRequestsInDateRange(today.plusDays(1), weekEnd);
        List<OffRequest> upcomingThisMonth = offRequestRepository.findApprovedRequestsInDateRange(today.plusDays(1), monthEnd);
        List<OffRequest> pendingRequests = offRequestRepository.findPendingRequests();
        
        return OffRequestDto.TimeBoardSummary.builder()
                .currentlyOnLeave(currentlyOnLeave.size())
                .upcomingLeaveThisWeek(upcomingThisWeek.size())
                .upcomingLeaveThisMonth(upcomingThisMonth.size())
                .pendingApproval(pendingRequests.size())
                .build();
    }
    
    /**
     * Create a new off request
     */
    public OffRequestDto.UserView createOffRequest(OffRequestDto.CreateRequest createRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        Employee employee = employeeRepository.findByAuthUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        // Validate request
        validateOffRequest(createRequest, currentUserId);
        
        // Calculate working days
        int workingDays = calculateWorkingDays(createRequest.getStartDate(), createRequest.getEndDate());
        
        OffRequest offRequest = OffRequest.builder()
                .employee(employee)
                .startDate(createRequest.getStartDate())
                .endDate(createRequest.getEndDate())
                .leaveType(createRequest.getLeaveType())
                .reason(createRequest.getReason())
                .daysRequested(workingDays)
                .isEmergency(createRequest.isEmergency())
                .status(OffRequest.RequestStatus.PENDING)
                .build();
        
        offRequest = offRequestRepository.save(offRequest);
        
        logger.info("Created off request for employee {} from {} to {}", 
                employee.getName(), createRequest.getStartDate(), createRequest.getEndDate());
        
        return convertToUserViewDto(offRequest);
    }
    
    /**
     * Get user's own off requests
     */
    public List<OffRequestDto.UserView> getMyOffRequests() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        List<OffRequest> requests = offRequestRepository.findByEmployeeAuthUserId(currentUserId);
        return requests.stream()
                .map(this::convertToUserViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get pending requests for approval (HR/Manager view)
     */
    public List<OffRequestDto.ManagementView> getPendingRequests() {
        List<OffRequest> requests = offRequestRepository.findPendingRequests();
        return requests.stream()
                .map(this::convertToManagementViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all requests for management (HR/Manager view)
     */
    public List<OffRequestDto.ManagementView> getAllRequestsForManagement() {
        List<OffRequest> requests = offRequestRepository.findAll();
        return requests.stream()
                .map(this::convertToManagementViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Approve or reject a request
     */
    public OffRequestDto.ManagementView processRequest(Long requestId, OffRequestDto.ApprovalAction action) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        if (!SecurityUtils.isCurrentUserHR() && !SecurityUtils.isCurrentUserAnyManager()) {
            throw new RuntimeException("Only HR and managers can process requests");
        }
        
        OffRequest request = offRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (request.getStatus() != OffRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be processed");
        }
        
        Employee approver = employeeRepository.findByAuthUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Approver employee not found"));
        
        if (action.getAction() == OffRequest.RequestStatus.APPROVED) {
            request.setStatus(OffRequest.RequestStatus.APPROVED);
            request.setApprovedBy(approver);
            request.setApprovedAt(LocalDateTime.now());
            logger.info("Approved off request {} by {}", requestId, approver.getName());
        } else if (action.getAction() == OffRequest.RequestStatus.REJECTED) {
            request.setStatus(OffRequest.RequestStatus.REJECTED);
            request.setApprovedBy(approver);
            request.setApprovedAt(LocalDateTime.now());
            request.setRejectionReason(action.getReason());
            logger.info("Rejected off request {} by {}", requestId, approver.getName());
        } else {
            throw new RuntimeException("Invalid action. Only APPROVED or REJECTED allowed");
        }
        
        request = offRequestRepository.save(request);
        return convertToManagementViewDto(request);
    }
    
    /**
     * Cancel a request (by the employee who created it)
     */
    public OffRequestDto.UserView cancelRequest(Long requestId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        OffRequest request = offRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!request.getEmployee().getAuthUserId().equals(currentUserId)) {
            throw new RuntimeException("You can only cancel your own requests");
        }
        
        if (request.getStatus() != OffRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be cancelled");
        }
        
        request.setStatus(OffRequest.RequestStatus.CANCELLED);
        request = offRequestRepository.save(request);
        
        logger.info("Cancelled off request {} by employee {}", requestId, currentUserId);
        
        return convertToUserViewDto(request);
    }
    
    /**
     * Get statistics
     */
    public OffRequestDto.Statistics getStatistics() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        int currentYear = LocalDate.now().getYear();
        LocalDate today = LocalDate.now();
        
        List<OffRequest> allRequests = offRequestRepository.findAll();
        
        long total = allRequests.size();
        long pending = allRequests.stream().filter(r -> r.getStatus() == OffRequest.RequestStatus.PENDING).count();
        long approved = allRequests.stream().filter(r -> r.getStatus() == OffRequest.RequestStatus.APPROVED).count();
        long rejected = allRequests.stream().filter(r -> r.getStatus() == OffRequest.RequestStatus.REJECTED).count();
        
        List<OffRequest> currentlyOnLeave = offRequestRepository.findCurrentActiveLeave(today);
        List<OffRequest> upcomingLeave = offRequestRepository.findUpcomingLeave(today);
        
        Long totalDaysUsed = 0L;
        Long totalDaysRequested = 0L;
        
        if (currentUserId != null) {
            totalDaysUsed = offRequestRepository.sumApprovedDaysByEmployeeAndYear(currentUserId, currentYear);
            totalDaysRequested = allRequests.stream()
                    .filter(r -> r.getEmployee().getAuthUserId().equals(currentUserId))
                    .filter(r -> r.getStartDate().getYear() == currentYear)
                    .mapToLong(r -> r.getDaysRequested())
                    .sum();
        }
        
        return OffRequestDto.Statistics.builder()
                .totalRequests(total)
                .pendingRequests(pending)
                .approvedRequests(approved)
                .rejectedRequests(rejected)
                .currentlyOnLeave((long) currentlyOnLeave.size())
                .upcomingLeave((long) upcomingLeave.size())
                .totalDaysUsedThisYear(totalDaysUsed)
                .totalDaysRequestedThisYear(totalDaysRequested)
                .build();
    }
    
    // Helper methods
    private void validateOffRequest(OffRequestDto.CreateRequest request, Long authUserId) {
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the past");
        }
        
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }
        
        // Check for overlapping requests
        List<OffRequest> overlapping = offRequestRepository.findOverlappingRequests(
                authUserId, request.getStartDate(), request.getEndDate());
        
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("You have overlapping time-off requests for these dates");
        }
    }
    
    private int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && 
                current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    private OffRequestDto.TimeBoard convertToTimeBoardDto(OffRequest request) {
        return OffRequestDto.TimeBoard.builder()
                .id(request.getId())
                .employeeName(request.getEmployee().getName())
                .employeeId(request.getEmployee().getEmployeeId())
                .authUserId(request.getEmployee().getAuthUserId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .leaveType(request.getLeaveType())
                .leaveTypeDisplay(request.getLeaveType().getDisplayName())
                .daysRequested(request.getDaysRequested())
                .isCurrentlyOnLeave(request.isCurrentlyOnLeave())
                .isEmergency(request.getIsEmergency())
                .avatarUrl(getAvatarUrl(request.getEmployee().getProfilePictureUrl()))
                .build();
    }
    
    private OffRequestDto.UserView convertToUserViewDto(OffRequest request) {
        boolean canCancel = request.getStatus() == OffRequest.RequestStatus.PENDING;
        boolean canEdit = request.getStatus() == OffRequest.RequestStatus.PENDING && 
                         request.getStartDate().isAfter(LocalDate.now());
        
        return OffRequestDto.UserView.builder()
                .id(request.getId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .leaveType(request.getLeaveType())
                .leaveTypeDisplay(request.getLeaveType().getDisplayName())
                .status(request.getStatus())
                .statusDisplay(request.getStatus().getDisplayName())
                .reason(request.getReason())
                .daysRequested(request.getDaysRequested())
                .isEmergency(request.getIsEmergency())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .approvedByName(request.getApprovedBy() != null ? request.getApprovedBy().getName() : null)
                .approvedAt(request.getApprovedAt())
                .rejectionReason(request.getRejectionReason())
                .isCurrentlyOnLeave(request.isCurrentlyOnLeave())
                .canCancel(canCancel)
                .canEdit(canEdit)
                .build();
    }
    
    private OffRequestDto.ManagementView convertToManagementViewDto(OffRequest request) {
        boolean canApprove = request.getStatus() == OffRequest.RequestStatus.PENDING;
        boolean canReject = request.getStatus() == OffRequest.RequestStatus.PENDING;
        
        // Get employee's year statistics
        int currentYear = LocalDate.now().getYear();
        Long usedDays = offRequestRepository.sumApprovedDaysByEmployeeAndYear(
                request.getEmployee().getAuthUserId(), currentYear);
        Long remainingDays = Math.max(0, 25 - usedDays); // Assuming 25 days annual leave
        
        return OffRequestDto.ManagementView.builder()
                .id(request.getId())
                .employeeName(request.getEmployee().getName())
                .employeeId(request.getEmployee().getEmployeeId())
                .authUserId(request.getEmployee().getAuthUserId())
                .employeeEmail(request.getEmployee().getEmail())
                .department(request.getEmployee().getDepartment() != null ? 
                           request.getEmployee().getDepartment().getName() : null)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .leaveType(request.getLeaveType())
                .leaveTypeDisplay(request.getLeaveType().getDisplayName())
                .status(request.getStatus())
                .statusDisplay(request.getStatus().getDisplayName())
                .reason(request.getReason())
                .daysRequested(request.getDaysRequested())
                .isEmergency(request.getIsEmergency())
                .createdAt(request.getCreatedAt())
                .approvedByName(request.getApprovedBy() != null ? request.getApprovedBy().getName() : null)
                .approvedAt(request.getApprovedAt())
                .rejectionReason(request.getRejectionReason())
                .isCurrentlyOnLeave(request.isCurrentlyOnLeave())
                .canApprove(canApprove)
                .canReject(canReject)
                .avatarUrl(getAvatarUrl(request.getEmployee().getProfilePictureUrl()))
                .usedDaysThisYear(usedDays)
                .remainingDaysThisYear(remainingDays)
                .build();
    }
    
    private String getAvatarUrl(String profilePictureUrl) {
        if (profilePictureUrl == null || profilePictureUrl.isEmpty()) {
            return null;
        }
        
        try {
            return fileStorageService.getAvatarUrl(profilePictureUrl);
        } catch (Exception e) {
            logger.warn("Failed to generate avatar URL for: {}", profilePictureUrl);
            return null;
        }
    }
} 