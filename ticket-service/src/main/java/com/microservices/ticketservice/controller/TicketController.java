package com.microservices.ticketservice.controller;

import com.microservices.ticketservice.config.HeaderAuthenticationFilter;
import com.microservices.ticketservice.dto.TicketCreateDto;
import com.microservices.ticketservice.dto.TicketUpdateDto;
import com.microservices.ticketservice.entity.Ticket;
import com.microservices.ticketservice.entity.Project;
import com.microservices.ticketservice.entity.TicketStatus;
import com.microservices.ticketservice.entity.TicketPriority;
import com.microservices.ticketservice.service.TicketService;
import com.microservices.ticketservice.service.ProjectService;
import com.microservices.ticketservice.service.ProjectService.ProjectCreateDto;
import com.microservices.ticketservice.service.ProjectService.ProjectUpdateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
@Tag(name = "Ticket Management", description = "APIs for managing tickets, assignments, and status updates")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;



    // Ticket Management Endpoints
    @Operation(summary = "Get all tickets", description = "Retrieve all tickets with pagination support")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved tickets",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<Ticket>> getAllTickets(
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        Page<Ticket> tickets = ticketService.getAllTickets(pageable);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        Optional<Ticket> ticket = ticketService.getTicketById(id);
        return ticket.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Ticket>> getTicketsByProject(@PathVariable Long projectId) {
        // Get the user ID from the authenticated user
        Long userId = getCurrentUserId();
        
        List<Ticket> tickets = ticketService.getTicketsByProject(projectId, userId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/assignee/{assigneeId}")
    public ResponseEntity<List<Ticket>> getTicketsByAssignee(@PathVariable Long assigneeId) {
        List<Ticket> tickets = ticketService.getTicketsByAssignee(assigneeId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Ticket>> getTicketsByStatus(@PathVariable TicketStatus status) {
        List<Ticket> tickets = ticketService.getTicketsByStatus(status);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Ticket>> searchTickets(
            @RequestParam String keyword,
            Pageable pageable) {
        Page<Ticket> tickets = ticketService.searchTickets(keyword, pageable);
        return ResponseEntity.ok(tickets);
    }

    @Operation(summary = "Create a new ticket", description = "Create a new ticket with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ticket created successfully",
            content = @Content(schema = @Schema(implementation = Ticket.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<Ticket> createTicket(
            @Parameter(description = "Ticket creation data", required = true)
            @Valid @RequestBody TicketCreateDto ticketDto) {
        
        // Get the user ID from the authenticated user
        Long userId = getCurrentUserId();
        
        Ticket createdTicket = ticketService.createTicket(ticketDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTicket);
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<Ticket> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketUpdateDto ticketDto) {
        
        // Get the user ID from the authenticated user
        Long userId = getCurrentUserId();
        
        Optional<Ticket> updatedTicket = ticketService.updateTicket(id, ticketDto, userId);
        return updatedTicket.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        // Get the user ID from the authenticated user
        Long userId = getCurrentUserId();
        
        boolean deleted = ticketService.deleteTicket(id, userId);
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }

    @PutMapping("/{ticketId}/assign/{assigneeId}")
    public ResponseEntity<Ticket> assignTicket(
            @PathVariable Long ticketId,
            @PathVariable Long assigneeId) {
        
        // Get the user ID from the authenticated user
        Long userId = getCurrentUserId();
        
        Optional<Ticket> assignedTicket = ticketService.assignTicket(ticketId, assigneeId, userId);
        return assignedTicket.map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<Ticket> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> statusUpdate) {
        
        // Get the user ID from the authenticated user
        Long userId = getCurrentUserId();
        
        try {
            TicketStatus status = TicketStatus.valueOf(statusUpdate.get("status"));
            Optional<Ticket> updatedTicket = ticketService.updateTicketStatus(ticketId, status, userId);
            return updatedTicket.map(ResponseEntity::ok)
                               .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{ticketId}/progress")
    public ResponseEntity<Ticket> updateTicketProgress(
            @PathVariable Long ticketId,
            @RequestBody Map<String, Integer> progressUpdate) {
        
        // Get the user ID from the authenticated user
        Long userId = getCurrentUserId();
        
        Integer finishPercentage = progressUpdate.get("finishPercentage");
        if (finishPercentage == null || finishPercentage < 0 || finishPercentage > 100) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<Ticket> updatedTicket = ticketService.updateTicketProgress(ticketId, finishPercentage, userId);
        return updatedTicket.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats/status/{status}")
    public ResponseEntity<Map<String, Object>> getTicketCountByStatus(@PathVariable TicketStatus status) {
        Long count = ticketService.getTicketCountByStatus(status);
        return ResponseEntity.ok(Map.of("count", count, "status", status.name()));
    }

    @GetMapping("/stats/assignee/{assigneeId}/open")
    public ResponseEntity<Map<String, Long>> getOpenTicketsByAssignee(@PathVariable Long assigneeId) {
        Long count = ticketService.getOpenTicketsByAssignee(assigneeId);
        return ResponseEntity.ok(Map.of("openTickets", count, "assigneeId", assigneeId));
    }

    @GetMapping("/stats/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectStats(@PathVariable Long projectId) {
        Long ticketCount = ticketService.getTicketCountByProject(projectId);
        Double progress = ticketService.getProjectProgress(projectId);
        return ResponseEntity.ok(Map.of(
            "ticketCount", ticketCount,
            "averageProgress", progress,
            "projectId", projectId
        ));
    }

    @GetMapping("/priorities")
    public ResponseEntity<TicketPriority[]> getTicketPriorities() {
        return ResponseEntity.ok(TicketPriority.values());
    }

    @GetMapping("/statuses")
    public ResponseEntity<TicketStatus[]> getTicketStatuses() {
        return ResponseEntity.ok(TicketStatus.values());
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof HeaderAuthenticationFilter.UserAuthentication) {
            HeaderAuthenticationFilter.UserAuthentication userAuth = 
                (HeaderAuthenticationFilter.UserAuthentication) authentication;
            return userAuth.getUserIdAsLong();
        }
        return 1L; // Default fallback
    }
} 