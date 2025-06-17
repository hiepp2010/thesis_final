package com.microservices.hrms.controller;

import com.microservices.hrms.dto.OffRequestDto;
import com.microservices.hrms.entity.OffRequest;
import com.microservices.hrms.service.OffRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hrms/time-off")
public class OffRequestController {
    
    private static final Logger logger = LoggerFactory.getLogger(OffRequestController.class);
    
    @Autowired
    private OffRequestService offRequestService;
    
    /**
     * Get time board - VISIBLE TO EVERYONE
     * Shows approved time-off requests so everyone knows who's off when
     */
    @GetMapping("/time-board")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTimeBoard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<OffRequestDto.TimeBoard> timeBoard = offRequestService.getTimeBoard(startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("timeBoard", timeBoard);
            response.put("totalPeopleOff", timeBoard.size());
            response.put("dateRange", Map.of(
                "start", startDate != null ? startDate : LocalDate.now().withDayOfMonth(1),
                "end", endDate != null ? endDate : LocalDate.now().plusMonths(3).withDayOfMonth(LocalDate.now().plusMonths(3).lengthOfMonth())
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching time board", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch time board: " + e.getMessage()));
        }
    }
    
    /**
     * Get time board summary for dashboard - VISIBLE TO EVERYONE
     */
    @GetMapping("/time-board/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTimeBoardSummary() {
        try {
            OffRequestDto.TimeBoardSummary summary = offRequestService.getTimeBoardSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error fetching time board summary", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch summary: " + e.getMessage()));
        }
    }
    
    /**
     * Create a new time-off request
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOffRequest(@RequestBody OffRequestDto.CreateRequest createRequest) {
        try {
            OffRequestDto.UserView request = offRequestService.createOffRequest(createRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Time-off request created successfully");
            response.put("request", request);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("Failed to create off request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating off request", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create request: " + e.getMessage()));
        }
    }
    
    /**
     * Get current user's own time-off requests
     */
    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyOffRequests() {
        try {
            List<OffRequestDto.UserView> requests = offRequestService.getMyOffRequests();
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests);
            response.put("totalRequests", requests.size());
            response.put("pendingRequests", requests.stream()
                    .mapToLong(r -> r.getStatus() == OffRequest.RequestStatus.PENDING ? 1 : 0)
                    .sum());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching user requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch requests: " + e.getMessage()));
        }
    }
    
    /**
     * Cancel a time-off request (by the user who created it)
     */
    @DeleteMapping("/request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelRequest(@PathVariable Long requestId) {
        try {
            OffRequestDto.UserView request = offRequestService.cancelRequest(requestId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Request cancelled successfully");
            response.put("request", request);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("Failed to cancel request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling request {}", requestId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to cancel request: " + e.getMessage()));
        }
    }
    
    /**
     * Get pending requests for approval (HR/Manager only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<?> getPendingRequests() {
        try {
            List<OffRequestDto.ManagementView> requests = offRequestService.getPendingRequests();
            
            Map<String, Object> response = new HashMap<>();
            response.put("pendingRequests", requests);
            response.put("totalPending", requests.size());
            response.put("emergencyRequests", requests.stream()
                    .mapToLong(r -> r.isEmergency() ? 1 : 0)
                    .sum());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching pending requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch pending requests: " + e.getMessage()));
        }
    }
    
    /**
     * Get all requests for management view (HR/Manager only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<?> getAllRequestsForManagement() {
        try {
            List<OffRequestDto.ManagementView> requests = offRequestService.getAllRequestsForManagement();
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests);
            response.put("totalRequests", requests.size());
            
            // Group by status
            Map<String, Long> statusCounts = new HashMap<>();
            requests.forEach(r -> {
                String status = r.getStatus().name();
                statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
            });
            response.put("statusCounts", statusCounts);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching all requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch requests: " + e.getMessage()));
        }
    }
    
    /**
     * Approve or reject a time-off request (HR/Manager only)
     */
    @PostMapping("/process/{requestId}")
    @PreAuthorize("hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<?> processRequest(@PathVariable Long requestId, 
                                          @RequestBody OffRequestDto.ApprovalAction action) {
        try {
            OffRequestDto.ManagementView request = offRequestService.processRequest(requestId, action);
            
            String actionText = action.getAction() == OffRequest.RequestStatus.APPROVED ? "approved" : "rejected";
            Map<String, Object> response = new HashMap<>();
            response.put("message", String.format("Request %s successfully", actionText));
            response.put("request", request);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("Failed to process request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing request {}", requestId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to process request: " + e.getMessage()));
        }
    }
    
    /**
     * Get statistics (HR/Manager view for detailed stats, user view for personal stats)
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getStatistics() {
        try {
            OffRequestDto.Statistics stats = offRequestService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching statistics", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Get available leave types
     */
    @GetMapping("/leave-types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLeaveTypes() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            OffRequest.LeaveType[] leaveTypes = OffRequest.LeaveType.values();
            Map<String, String> typeMap = new HashMap<>();
            
            for (OffRequest.LeaveType type : leaveTypes) {
                typeMap.put(type.name(), type.getDisplayName());
            }
            
            response.put("leaveTypes", typeMap);
            response.put("description", Map.of(
                "VACATION", "Annual vacation leave",
                "SICK_LEAVE", "Medical leave for illness",
                "PERSONAL", "Personal time off",
                "MATERNITY", "Maternity leave",
                "PATERNITY", "Paternity leave",
                "BEREAVEMENT", "Bereavement leave",
                "EMERGENCY", "Emergency leave",
                "UNPAID", "Unpaid leave",
                "STUDY", "Study/Educational leave",
                "OTHER", "Other types of leave"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching leave types", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch leave types: " + e.getMessage()));
        }
    }
} 