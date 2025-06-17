package com.microservices.hrms.service;

import com.microservices.hrms.dto.OvertimeRequestDto;
import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.entity.OvertimeRequest;
import com.microservices.hrms.repository.EmployeeRepository;
import com.microservices.hrms.repository.OvertimeRequestRepository;
import com.microservices.hrms.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OvertimeRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(OvertimeRequestService.class);
    private static final BigDecimal DEFAULT_OVERTIME_RATE = new BigDecimal("25.00"); // Default rate per hour
    
    @Autowired
    private OvertimeRequestRepository overtimeRequestRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Get overtime board - approved requests visible to everyone
     */
    public List<OvertimeRequestDto.OvertimeBoard> getOvertimeBoard(LocalDate startDate, LocalDate endDate) {
        List<OvertimeRequest> requests;
        
        if (startDate != null && endDate != null) {
            requests = overtimeRequestRepository.findApprovedRequestsInDateRange(startDate, endDate);
        } else {
            // Default to show current week + 4 weeks ahead
            LocalDate defaultStart = LocalDate.now();
            LocalDate defaultEnd = defaultStart.plusWeeks(4);
            requests = overtimeRequestRepository.findApprovedRequestsInDateRange(defaultStart, defaultEnd);
        }
        
        return requests.stream()
                .map(this::convertToOvertimeBoardDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get overtime board summary for dashboard
     */
    public OvertimeRequestDto.OvertimeBoardSummary getOvertimeBoardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekEnd = today.plusWeeks(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        
        List<OvertimeRequest> todaysOvertime = overtimeRequestRepository.findTodaysOvertime(today);
        List<OvertimeRequest> tomorrowsOvertime = overtimeRequestRepository.findTodaysOvertime(tomorrow);
        List<OvertimeRequest> thisWeeksOvertime = overtimeRequestRepository.findApprovedRequestsInDateRange(today, weekEnd);
        List<OvertimeRequest> pendingRequests = overtimeRequestRepository.findPendingRequests();
        List<OvertimeRequest> urgentRequests = overtimeRequestRepository.findUrgentPendingRequests();
        
        BigDecimal thisWeeksHours = thisWeeksOvertime.stream()
                .map(OvertimeRequest::getHoursRequested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal thisMonthsHours = overtimeRequestRepository.sumApprovedHoursByMonth(
                today.getMonthValue(), today.getYear());
        
        return OvertimeRequestDto.OvertimeBoardSummary.builder()
                .todaysOvertime(todaysOvertime.size())
                .tomorrowsOvertime(tomorrowsOvertime.size())
                .thisWeeksOvertime(thisWeeksOvertime.size())
                .pendingApproval(pendingRequests.size())
                .urgentRequests(urgentRequests.size())
                .thisWeeksHours(thisWeeksHours)
                .thisMonthsHours(thisMonthsHours)
                .build();
    }
    
    /**
     * Create a new overtime request
     */
    public OvertimeRequestDto.UserView createOvertimeRequest(OvertimeRequestDto.CreateRequest createRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        Employee employee = employeeRepository.findByAuthUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        // Validate request
        validateOvertimeRequest(createRequest, currentUserId);
        
        // Calculate hours if not provided but start/end times are given
        BigDecimal hoursRequested = createRequest.getHoursRequested();
        if (hoursRequested == null && createRequest.getStartTime() != null && createRequest.getEndTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(createRequest.getStartTime(), createRequest.getEndTime());
            hoursRequested = new BigDecimal(minutes).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
        }
        
        if (hoursRequested == null || hoursRequested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Hours requested must be greater than 0");
        }
        
        OvertimeRequest overtimeRequest = OvertimeRequest.builder()
                .employee(employee)
                .overtimeDate(createRequest.getOvertimeDate())
                .startTime(createRequest.getStartTime())
                .endTime(createRequest.getEndTime())
                .hoursRequested(hoursRequested)
                .overtimeType(createRequest.getOvertimeType())
                .reason(createRequest.getReason())
                .projectOrTask(createRequest.getProjectOrTask())
                .isUrgent(createRequest.isUrgent())
                .status(OvertimeRequest.RequestStatus.PENDING)
                .build();
        
        overtimeRequest = overtimeRequestRepository.save(overtimeRequest);
        
        logger.info("Created overtime request for employee {} for {} hours on {}", 
                employee.getName(), hoursRequested, createRequest.getOvertimeDate());
        
        return convertToUserViewDto(overtimeRequest);
    }
    
    /**
     * Get user's own overtime requests
     */
    public List<OvertimeRequestDto.UserView> getMyOvertimeRequests() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        List<OvertimeRequest> requests = overtimeRequestRepository.findByEmployeeAuthUserId(currentUserId);
        return requests.stream()
                .map(this::convertToUserViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get pending requests for approval (HR/Manager view)
     */
    public List<OvertimeRequestDto.ManagementView> getPendingRequests() {
        List<OvertimeRequest> requests = overtimeRequestRepository.findPendingRequests();
        return requests.stream()
                .map(this::convertToManagementViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get urgent pending requests
     */
    public List<OvertimeRequestDto.ManagementView> getUrgentPendingRequests() {
        List<OvertimeRequest> requests = overtimeRequestRepository.findUrgentPendingRequests();
        return requests.stream()
                .map(this::convertToManagementViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all requests for management (HR/Manager view)
     */
    public List<OvertimeRequestDto.ManagementView> getAllRequestsForManagement() {
        List<OvertimeRequest> requests = overtimeRequestRepository.findAll();
        return requests.stream()
                .map(this::convertToManagementViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Approve or reject a request
     */
    public OvertimeRequestDto.ManagementView processRequest(Long requestId, OvertimeRequestDto.ApprovalAction action) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        if (!SecurityUtils.isCurrentUserHR() && !SecurityUtils.isCurrentUserAnyManager()) {
            throw new RuntimeException("Only HR and managers can process requests");
        }
        
        OvertimeRequest request = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (request.getStatus() != OvertimeRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be processed");
        }
        
        Employee approver = employeeRepository.findByAuthUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Approver employee not found"));
        
        if (action.getAction() == OvertimeRequest.RequestStatus.APPROVED) {
            request.setStatus(OvertimeRequest.RequestStatus.APPROVED);
            request.setApprovedBy(approver);
            request.setApprovedAt(LocalDateTime.now());
            
            // Set estimated rate if provided, otherwise use default
            BigDecimal rate = action.getEstimatedRate() != null ? action.getEstimatedRate() : DEFAULT_OVERTIME_RATE;
            request.setEstimatedRate(rate);
            
            logger.info("Approved overtime request {} by {}", requestId, approver.getName());
        } else if (action.getAction() == OvertimeRequest.RequestStatus.REJECTED) {
            request.setStatus(OvertimeRequest.RequestStatus.REJECTED);
            request.setApprovedBy(approver);
            request.setApprovedAt(LocalDateTime.now());
            request.setRejectionReason(action.getReason());
            logger.info("Rejected overtime request {} by {}", requestId, approver.getName());
        } else {
            throw new RuntimeException("Invalid action. Only APPROVED or REJECTED allowed");
        }
        
        request = overtimeRequestRepository.save(request);
        return convertToManagementViewDto(request);
    }
    
    /**
     * Cancel a request (by the employee who created it)
     */
    public OvertimeRequestDto.UserView cancelRequest(Long requestId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        OvertimeRequest request = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!request.getEmployee().getAuthUserId().equals(currentUserId)) {
            throw new RuntimeException("You can only cancel your own requests");
        }
        
        if (request.getStatus() != OvertimeRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be cancelled");
        }
        
        request.setStatus(OvertimeRequest.RequestStatus.CANCELLED);
        request = overtimeRequestRepository.save(request);
        
        logger.info("Cancelled overtime request {} by employee {}", requestId, currentUserId);
        
        return convertToUserViewDto(request);
    }
    
    /**
     * Mark overtime as completed
     */
    public OvertimeRequestDto.UserView completeOvertime(Long requestId, OvertimeRequestDto.CompletionAction completion) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        OvertimeRequest request = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!request.getEmployee().getAuthUserId().equals(currentUserId)) {
            throw new RuntimeException("You can only complete your own overtime");
        }
        
        if (request.getStatus() != OvertimeRequest.RequestStatus.APPROVED) {
            throw new RuntimeException("Only approved overtime can be marked as completed");
        }
        
        request.setStatus(OvertimeRequest.RequestStatus.COMPLETED);
        request.setActualHoursWorked(completion.getActualHoursWorked());
        request = overtimeRequestRepository.save(request);
        
        logger.info("Completed overtime request {} with {} actual hours", requestId, completion.getActualHoursWorked());
        
        return convertToUserViewDto(request);
    }
    
    /**
     * Get statistics
     */
    public OvertimeRequestDto.Statistics getStatistics() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        LocalDate today = LocalDate.now();
        
        List<OvertimeRequest> allRequests = overtimeRequestRepository.findAll();
        
        long total = allRequests.size();
        long pending = allRequests.stream().filter(r -> r.getStatus() == OvertimeRequest.RequestStatus.PENDING).count();
        long approved = allRequests.stream().filter(r -> r.getStatus() == OvertimeRequest.RequestStatus.APPROVED).count();
        long rejected = allRequests.stream().filter(r -> r.getStatus() == OvertimeRequest.RequestStatus.REJECTED).count();
        long urgent = allRequests.stream().filter(r -> r.getIsUrgent() && r.getStatus() == OvertimeRequest.RequestStatus.PENDING).count();
        
        List<OvertimeRequest> todaysOvertime = overtimeRequestRepository.findTodaysOvertime(today);
        List<OvertimeRequest> upcomingOvertime = overtimeRequestRepository.findUpcomingOvertime(today);
        
        BigDecimal totalHoursThisYear = BigDecimal.ZERO;
        BigDecimal totalHoursThisMonth = overtimeRequestRepository.sumApprovedHoursByMonth(currentMonth, currentYear);
        BigDecimal totalEstimatedCostThisMonth = BigDecimal.ZERO;
        BigDecimal averageHoursPerRequest = BigDecimal.ZERO;
        
        if (currentUserId != null) {
            totalHoursThisYear = overtimeRequestRepository.sumApprovedHoursByEmployeeAndYear(currentUserId, currentYear);
        }
        
        if (total > 0) {
            BigDecimal totalHours = allRequests.stream()
                    .map(OvertimeRequest::getHoursRequested)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageHoursPerRequest = totalHours.divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
        }
        
        // Calculate estimated cost for this month
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        totalEstimatedCostThisMonth = overtimeRequestRepository.calculateEstimatedCostInDateRange(monthStart, monthEnd);
        
        return OvertimeRequestDto.Statistics.builder()
                .totalRequests(total)
                .pendingRequests(pending)
                .approvedRequests(approved)
                .rejectedRequests(rejected)
                .urgentRequests(urgent)
                .todaysOvertime((long) todaysOvertime.size())
                .upcomingOvertime((long) upcomingOvertime.size())
                .totalHoursThisYear(totalHoursThisYear)
                .totalHoursThisMonth(totalHoursThisMonth)
                .totalEstimatedCostThisMonth(totalEstimatedCostThisMonth)
                .averageHoursPerRequest(averageHoursPerRequest)
                .build();
    }
    
    // Helper methods
    private void validateOvertimeRequest(OvertimeRequestDto.CreateRequest request, Long authUserId) {
        if (request.getOvertimeDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Overtime date cannot be in the past");
        }
        
        // Check for overlapping requests if times are specified
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().equals(request.getStartTime())) {
                throw new RuntimeException("End time must be after start time");
            }
            
            List<OvertimeRequest> overlapping = overtimeRequestRepository.findOverlappingRequests(
                    authUserId, request.getOvertimeDate(), request.getStartTime(), request.getEndTime());
            
            if (!overlapping.isEmpty()) {
                throw new RuntimeException("You have overlapping overtime requests for this time period");
            }
        }
        
        // Validate hours
        if (request.getHoursRequested() != null && request.getHoursRequested().compareTo(new BigDecimal("12")) > 0) {
            throw new RuntimeException("Overtime cannot exceed 12 hours per day");
        }
    }
    
    private OvertimeRequestDto.OvertimeBoard convertToOvertimeBoardDto(OvertimeRequest request) {
        return OvertimeRequestDto.OvertimeBoard.builder()
                .id(request.getId())
                .employeeName(request.getEmployee().getName())
                .employeeId(request.getEmployee().getEmployeeId())
                .authUserId(request.getEmployee().getAuthUserId())
                .overtimeDate(request.getOvertimeDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .hoursRequested(request.getHoursRequested())
                .overtimeType(request.getOvertimeType())
                .overtimeTypeDisplay(request.getOvertimeType().getDisplayName())
                .projectOrTask(request.getProjectOrTask())
                .isToday(request.isToday())
                .isWeekend(request.isWeekend())
                .isUrgent(request.getIsUrgent())
                .avatarUrl(getAvatarUrl(request.getEmployee().getProfilePictureUrl()))
                .build();
    }
    
    private OvertimeRequestDto.UserView convertToUserViewDto(OvertimeRequest request) {
        boolean canCancel = request.getStatus() == OvertimeRequest.RequestStatus.PENDING;
        boolean canEdit = request.getStatus() == OvertimeRequest.RequestStatus.PENDING && 
                         request.getOvertimeDate().isAfter(LocalDate.now());
        
        BigDecimal estimatedCost = BigDecimal.ZERO;
        if (request.getEstimatedRate() != null && request.getHoursRequested() != null) {
            estimatedCost = request.getEstimatedRate().multiply(request.getHoursRequested());
        }
        
        return OvertimeRequestDto.UserView.builder()
                .id(request.getId())
                .overtimeDate(request.getOvertimeDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .hoursRequested(request.getHoursRequested())
                .overtimeType(request.getOvertimeType())
                .overtimeTypeDisplay(request.getOvertimeType().getDisplayName())
                .status(request.getStatus())
                .statusDisplay(request.getStatus().getDisplayName())
                .reason(request.getReason())
                .projectOrTask(request.getProjectOrTask())
                .isUrgent(request.getIsUrgent())
                .estimatedRate(request.getEstimatedRate())
                .estimatedCost(estimatedCost)
                .actualHoursWorked(request.getActualHoursWorked())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .approvedByName(request.getApprovedBy() != null ? request.getApprovedBy().getName() : null)
                .approvedAt(request.getApprovedAt())
                .rejectionReason(request.getRejectionReason())
                .isToday(request.isToday())
                .canCancel(canCancel)
                .canEdit(canEdit)
                .build();
    }
    
    private OvertimeRequestDto.ManagementView convertToManagementViewDto(OvertimeRequest request) {
        boolean canApprove = request.getStatus() == OvertimeRequest.RequestStatus.PENDING;
        boolean canReject = request.getStatus() == OvertimeRequest.RequestStatus.PENDING;
        
        // Get employee's year and month statistics
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        BigDecimal totalHoursThisYear = overtimeRequestRepository.sumApprovedHoursByEmployeeAndYear(
                request.getEmployee().getAuthUserId(), currentYear);
        BigDecimal totalHoursThisMonth = overtimeRequestRepository.sumApprovedHoursByMonth(currentMonth, currentYear);
        
        BigDecimal estimatedCost = BigDecimal.ZERO;
        if (request.getEstimatedRate() != null && request.getHoursRequested() != null) {
            estimatedCost = request.getEstimatedRate().multiply(request.getHoursRequested());
        }
        
        return OvertimeRequestDto.ManagementView.builder()
                .id(request.getId())
                .employeeName(request.getEmployee().getName())
                .employeeId(request.getEmployee().getEmployeeId())
                .authUserId(request.getEmployee().getAuthUserId())
                .employeeEmail(request.getEmployee().getEmail())
                .department(request.getEmployee().getDepartment() != null ? 
                           request.getEmployee().getDepartment().getName() : null)
                .overtimeDate(request.getOvertimeDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .hoursRequested(request.getHoursRequested())
                .overtimeType(request.getOvertimeType())
                .overtimeTypeDisplay(request.getOvertimeType().getDisplayName())
                .status(request.getStatus())
                .statusDisplay(request.getStatus().getDisplayName())
                .reason(request.getReason())
                .projectOrTask(request.getProjectOrTask())
                .isUrgent(request.getIsUrgent())
                .estimatedRate(request.getEstimatedRate())
                .estimatedCost(estimatedCost)
                .actualHoursWorked(request.getActualHoursWorked())
                .createdAt(request.getCreatedAt())
                .approvedByName(request.getApprovedBy() != null ? request.getApprovedBy().getName() : null)
                .approvedAt(request.getApprovedAt())
                .rejectionReason(request.getRejectionReason())
                .isToday(request.isToday())
                .canApprove(canApprove)
                .canReject(canReject)
                .avatarUrl(getAvatarUrl(request.getEmployee().getProfilePictureUrl()))
                .totalOvertimeHoursThisYear(totalHoursThisYear)
                .totalOvertimeHoursThisMonth(totalHoursThisMonth)
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