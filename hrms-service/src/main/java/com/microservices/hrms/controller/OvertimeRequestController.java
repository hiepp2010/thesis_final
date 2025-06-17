package com.microservices.hrms.controller;

import com.microservices.hrms.dto.OvertimeRequestDto;
import com.microservices.hrms.entity.OvertimeRequest;
import com.microservices.hrms.service.OvertimeRequestService;
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
@RequestMapping("/api/hrms/overtime")
public class OvertimeRequestController {
    
    private static final Logger logger = LoggerFactory.getLogger(OvertimeRequestController.class);
    
    @Autowired
    private OvertimeRequestService overtimeRequestService;
    
    /**
     * Get overtime board - VISIBLE TO EVERYONE
     * Shows approved overtime requests so everyone knows who's working overtime when
     */
    @GetMapping("/overtime-board")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOvertimeBoard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<OvertimeRequestDto.OvertimeBoard> overtimeBoard = overtimeRequestService.getOvertimeBoard(startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("overtimeBoard", overtimeBoard);
            response.put("totalPeopleWorking", overtimeBoard.size());
            response.put("dateRange", Map.of(
                "start", startDate != null ? startDate : LocalDate.now(),
                "end", endDate != null ? endDate : LocalDate.now().plusWeeks(4)
            ));
            
            // Group by date for better visualization
            Map<LocalDate, List<OvertimeRequestDto.OvertimeBoard>> groupedByDate = overtimeBoard.stream()
                    .collect(java.util.stream.Collectors.groupingBy(OvertimeRequestDto.OvertimeBoard::getOvertimeDate));
            response.put("groupedByDate", groupedByDate);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching overtime board", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch overtime board: " + e.getMessage()));
        }
    }
    
    /**
     * Get overtime board summary for dashboard - VISIBLE TO EVERYONE
     */
    @GetMapping("/overtime-board/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOvertimeBoardSummary() {
        try {
            OvertimeRequestDto.OvertimeBoardSummary summary = overtimeRequestService.getOvertimeBoardSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error fetching overtime board summary", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch summary: " + e.getMessage()));
        }
    }
    
    /**
     * Create a new overtime request
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOvertimeRequest(@RequestBody OvertimeRequestDto.CreateRequest createRequest) {
        try {
            OvertimeRequestDto.UserView request = overtimeRequestService.createOvertimeRequest(createRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Overtime request created successfully");
            response.put("request", request);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("Failed to create overtime request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating overtime request", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create request: " + e.getMessage()));
        }
    }
    
    /**
     * Get current user's own overtime requests
     */
    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyOvertimeRequests() {
        try {
            List<OvertimeRequestDto.UserView> requests = overtimeRequestService.getMyOvertimeRequests();
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests);
            response.put("totalRequests", requests.size());
            response.put("pendingRequests", requests.stream()
                    .mapToLong(r -> r.getStatus() == OvertimeRequest.RequestStatus.PENDING ? 1 : 0)
                    .sum());
            response.put("totalHoursRequested", requests.stream()
                    .map(OvertimeRequestDto.UserView::getHoursRequested)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching user overtime requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch requests: " + e.getMessage()));
        }
    }
    
    /**
     * Cancel an overtime request (by the user who created it)
     */
    @DeleteMapping("/request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelRequest(@PathVariable Long requestId) {
        try {
            OvertimeRequestDto.UserView request = overtimeRequestService.cancelRequest(requestId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Overtime request cancelled successfully");
            response.put("request", request);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("Failed to cancel overtime request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling overtime request {}", requestId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to cancel request: " + e.getMessage()));
        }
    }
    
    /**
     * Mark overtime as completed (by the user who worked it)
     */
    @PostMapping("/complete/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> completeOvertime(@PathVariable Long requestId, 
                                            @RequestBody OvertimeRequestDto.CompletionAction completion) {
        try {
            OvertimeRequestDto.UserView request = overtimeRequestService.completeOvertime(requestId, completion);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Overtime marked as completed");
            response.put("request", request);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("Failed to complete overtime {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error completing overtime {}", requestId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to complete overtime: " + e.getMessage()));
        }
    }
    
    /**
     * Get pending requests for approval (HR/Manager only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<?> getPendingRequests() {
        try {
            List<OvertimeRequestDto.ManagementView> requests = overtimeRequestService.getPendingRequests();
            
            Map<String, Object> response = new HashMap<>();
            response.put("pendingRequests", requests);
            response.put("totalPending", requests.size());
            response.put("urgentRequests", requests.stream()
                    .mapToLong(r -> r.isUrgent() ? 1 : 0)
                    .sum());
            response.put("totalHoursPending", requests.stream()
                    .map(OvertimeRequestDto.ManagementView::getHoursRequested)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching pending overtime requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch pending requests: " + e.getMessage()));
        }
    }
    
    /**
     * Get urgent pending requests (HR/Manager only)
     */
    @GetMapping("/urgent")
    @PreAuthorize("hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<?> getUrgentPendingRequests() {
        try {
            List<OvertimeRequestDto.ManagementView> requests = overtimeRequestService.getUrgentPendingRequests();
            
            Map<String, Object> response = new HashMap<>();
            response.put("urgentRequests", requests);
            response.put("totalUrgent", requests.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching urgent overtime requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch urgent requests: " + e.getMessage()));
        }
    }
    
    /**
     * Get all requests for management view (HR/Manager only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<?> getAllRequestsForManagement() {
        try {
            List<OvertimeRequestDto.ManagementView> requests = overtimeRequestService.getAllRequestsForManagement();
            
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
            
            // Group by overtime type
            Map<String, Long> typeCounts = new HashMap<>();
            requests.forEach(r -> {
                String type = r.getOvertimeType().name();
                typeCounts.put(type, typeCounts.getOrDefault(type, 0L) + 1);
            });
            response.put("typeCounts", typeCounts);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching all overtime requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch requests: " + e.getMessage()));
        }
    }
    
    /**
     * Approve or reject an overtime request (HR/Manager only)
     */
    @PostMapping("/process/{requestId}")
    @PreAuthorize("hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<?> processRequest(@PathVariable Long requestId, 
                                          @RequestBody OvertimeRequestDto.ApprovalAction action) {
        try {
            OvertimeRequestDto.ManagementView request = overtimeRequestService.processRequest(requestId, action);
            
            String actionText = action.getAction() == OvertimeRequest.RequestStatus.APPROVED ? "approved" : "rejected";
            Map<String, Object> response = new HashMap<>();
            response.put("message", String.format("Overtime request %s successfully", actionText));
            response.put("request", request);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("Failed to process overtime request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing overtime request {}", requestId, e);
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
            OvertimeRequestDto.Statistics stats = overtimeRequestService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching overtime statistics", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Get available overtime types
     */
    @GetMapping("/overtime-types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOvertimeTypes() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            OvertimeRequest.OvertimeType[] overtimeTypes = OvertimeRequest.OvertimeType.values();
            Map<String, String> typeMap = new HashMap<>();
            
            for (OvertimeRequest.OvertimeType type : overtimeTypes) {
                typeMap.put(type.name(), type.getDisplayName());
            }
            
            response.put("overtimeTypes", typeMap);
            response.put("description", Map.of(
                "REGULAR", "Regular overtime work",
                "WEEKEND", "Weekend work",
                "HOLIDAY", "Holiday work",
                "EMERGENCY", "Emergency overtime",
                "PROJECT_DEADLINE", "Project deadline work",
                "MAINTENANCE", "System maintenance work",
                "TRAINING", "Training or development",
                "CLIENT_REQUEST", "Client-requested work",
                "OTHER", "Other overtime work"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching overtime types", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch overtime types: " + e.getMessage()));
        }
    }
} 