package com.microservices.ticketservice.service;

import com.microservices.ticketservice.dto.TicketCreateDto;
import com.microservices.ticketservice.dto.TicketUpdateDto;
import com.microservices.ticketservice.entity.Ticket;
import com.microservices.ticketservice.entity.TicketStatus;
import com.microservices.ticketservice.entity.TicketPriority;
import com.microservices.ticketservice.entity.User;
import com.microservices.ticketservice.entity.Project;
import com.microservices.ticketservice.repository.TicketRepository;
import com.microservices.ticketservice.repository.UserRepository;
import com.microservices.ticketservice.repository.ProjectRepository;
import com.microservices.ticketservice.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TICKET_TOPIC = "ticket-events";

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Page<Ticket> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable);
    }

    public Optional<Ticket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    public List<Ticket> getTicketsByAssignee(Long assigneeId) {
        Optional<User> user = userRepository.findById(assigneeId);
        return user.map(ticketRepository::findByAssignee).orElse(List.of());
    }

    public List<Ticket> getTicketsByProject(Long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.map(ticketRepository::findByProject).orElse(List.of());
    }

    /**
     * Get tickets by project with user access control
     */
    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByProject(Long projectId, Long userId) {
        try {
            // Check if user is project owner or member
            if (!projectService.isUserProjectMember(userId, projectId)) {
                throw new RuntimeException("Access denied. User is not a member of this project.");
            }
            
            Optional<Project> project = projectRepository.findById(projectId);
            return project.map(ticketRepository::findByProject).orElse(List.of());
        } catch (Exception e) {
            // Log the error and rethrow
            System.err.println("Error getting tickets for project " + projectId + ": " + e.getMessage());
            throw e;
        }
    }

    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status);
    }

    public Page<Ticket> searchTickets(String keyword, Pageable pageable) {
        return ticketRepository.findByKeyword(keyword, pageable);
    }

    public Ticket createTicket(TicketCreateDto ticketDto, Long userId) {
        // Get project
        Project project = projectRepository.findById(ticketDto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + ticketDto.getProjectId()));

        // Check if user is project owner - only owners can create tickets
        if (!projectService.isUserProjectOwner(userId, ticketDto.getProjectId())) {
            throw new RuntimeException("Access denied. Only project owners can create tickets.");
        }

        Ticket ticket = Ticket.builder()
                .title(ticketDto.getTitle())
                .description(ticketDto.getDescription())
                .priority(ticketDto.getPriority())
                .project(project)
                .dueDate(ticketDto.getDueDate())
                .finishPercentage(0)
                .build();

        // Set assignee if provided
        if (ticketDto.getAssigneeId() != null) {
            User assignee = userRepository.findById(ticketDto.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + ticketDto.getAssigneeId()));
            
            // Check if assignee is project member
            if (!projectService.isUserProjectMember(ticketDto.getAssigneeId(), ticketDto.getProjectId())) {
                throw new RuntimeException("Cannot assign ticket to user who is not a project member.");
            }
            
            ticket.setAssignee(assignee);
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        // Send Kafka event (disabled for now due to serialization issues)
        // publishTicketEvent("TICKET_CREATED", savedTicket);

        return savedTicket;
    }

    public Optional<Ticket> updateTicket(Long id, TicketUpdateDto ticketDto, Long userId) {
        return ticketRepository.findById(id).map(ticket -> {
            // Check if user is project owner - only owners can update tickets
            if (!projectService.isUserProjectOwner(userId, ticket.getProject().getId())) {
                throw new RuntimeException("Access denied. Only project owners can update tickets.");
            }
            
            if (ticketDto.getTitle() != null) {
                ticket.setTitle(ticketDto.getTitle());
            }
            if (ticketDto.getDescription() != null) {
                ticket.setDescription(ticketDto.getDescription());
            }
            if (ticketDto.getStatus() != null) {
                ticket.setStatus(ticketDto.getStatus());
            }
            if (ticketDto.getPriority() != null) {
                ticket.setPriority(ticketDto.getPriority());
            }
            if (ticketDto.getAssigneeId() != null) {
                User assignee = userRepository.findById(ticketDto.getAssigneeId())
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + ticketDto.getAssigneeId()));
                
                // Check if assignee is project member
                if (!projectService.isUserProjectMember(ticketDto.getAssigneeId(), ticket.getProject().getId())) {
                    throw new RuntimeException("Cannot assign ticket to user who is not a project member.");
                }
                
                ticket.setAssignee(assignee);
            }
            if (ticketDto.getDueDate() != null) {
                ticket.setDueDate(ticketDto.getDueDate());
            }
            if (ticketDto.getFinishPercentage() != null) {
                ticket.setFinishPercentage(ticketDto.getFinishPercentage());
            }

            Ticket updatedTicket = ticketRepository.save(ticket);

            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_UPDATED", updatedTicket);

            return updatedTicket;
        });
    }

    public boolean deleteTicket(Long id, Long userId) {
        return ticketRepository.findById(id).map(ticket -> {
            // Check if user is project owner - only owners can delete tickets
            if (!projectService.isUserProjectOwner(userId, ticket.getProject().getId())) {
                throw new RuntimeException("Access denied. Only project owners can delete tickets.");
            }
            
            ticketRepository.delete(ticket);
            
            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_DELETED", ticket);
            
            return true;
        }).orElse(false);
    }

    // Legacy method without access control - kept for backward compatibility
    public Optional<Ticket> updateTicket(Long id, TicketUpdateDto ticketDto) {
        return ticketRepository.findById(id).map(ticket -> {
            if (ticketDto.getTitle() != null) {
                ticket.setTitle(ticketDto.getTitle());
            }
            if (ticketDto.getDescription() != null) {
                ticket.setDescription(ticketDto.getDescription());
            }
            if (ticketDto.getStatus() != null) {
                ticket.setStatus(ticketDto.getStatus());
            }
            if (ticketDto.getPriority() != null) {
                ticket.setPriority(ticketDto.getPriority());
            }
            if (ticketDto.getAssigneeId() != null) {
                User assignee = userRepository.findById(ticketDto.getAssigneeId())
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + ticketDto.getAssigneeId()));
                ticket.setAssignee(assignee);
            }
            if (ticketDto.getDueDate() != null) {
                ticket.setDueDate(ticketDto.getDueDate());
            }
            if (ticketDto.getFinishPercentage() != null) {
                ticket.setFinishPercentage(ticketDto.getFinishPercentage());
            }

            Ticket updatedTicket = ticketRepository.save(ticket);

            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_UPDATED", updatedTicket);

            return updatedTicket;
        });
    }

    // Legacy method without access control - kept for backward compatibility  
    public boolean deleteTicket(Long id) {
        return ticketRepository.findById(id).map(ticket -> {
            ticketRepository.delete(ticket);
            
            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_DELETED", ticket);
            
            return true;
        }).orElse(false);
    }

    public Optional<Ticket> assignTicket(Long ticketId, Long assigneeId, Long userId) {
        return ticketRepository.findById(ticketId).map(ticket -> {
            // Check if user is project owner - only owners can assign tickets
            if (!projectService.isUserProjectOwner(userId, ticket.getProject().getId())) {
                throw new RuntimeException("Access denied. Only project owners can assign tickets.");
            }
            
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + assigneeId));
            
            // Check if assignee is project member
            if (!projectService.isUserProjectMember(assigneeId, ticket.getProject().getId())) {
                throw new RuntimeException("Cannot assign ticket to user who is not a project member.");
            }
            
            ticket.setAssignee(assignee);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            
            Ticket assignedTicket = ticketRepository.save(ticket);
            
            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_ASSIGNED", assignedTicket);
            
            return assignedTicket;
        });
    }

    // Legacy method without access control - kept for backward compatibility
    public Optional<Ticket> assignTicket(Long ticketId, Long assigneeId) {
        return ticketRepository.findById(ticketId).map(ticket -> {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + assigneeId));
            
            ticket.setAssignee(assignee);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            
            Ticket assignedTicket = ticketRepository.save(ticket);
            
            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_ASSIGNED", assignedTicket);
            
            return assignedTicket;
        });
    }

    public Optional<Ticket> updateTicketStatus(Long ticketId, TicketStatus status, Long userId) {
        return ticketRepository.findById(ticketId).map(ticket -> {
            // Check if user is project owner - only owners can update ticket status
            if (!projectService.isUserProjectOwner(userId, ticket.getProject().getId())) {
                throw new RuntimeException("Access denied. Only project owners can update ticket status.");
            }
            
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(status);
            
            Ticket updatedTicket = ticketRepository.save(ticket);
            
            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_STATUS_CHANGED", updatedTicket);
            
            return updatedTicket;
        });
    }

    // Legacy method without access control - kept for backward compatibility
    public Optional<Ticket> updateTicketStatus(Long ticketId, TicketStatus status) {
        return ticketRepository.findById(ticketId).map(ticket -> {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(status);
            
            Ticket updatedTicket = ticketRepository.save(ticket);
            
            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_STATUS_CHANGED", updatedTicket);
            
            return updatedTicket;
        });
    }

    public Optional<Ticket> updateTicketProgress(Long ticketId, Integer finishPercentage, Long userId) {
        return ticketRepository.findById(ticketId).map(ticket -> {
            // Check if user is project owner - only owners can update ticket progress
            if (!projectService.isUserProjectOwner(userId, ticket.getProject().getId())) {
                throw new RuntimeException("Access denied. Only project owners can update ticket progress.");
            }
            
            ticket.setFinishPercentage(finishPercentage);
            
            // Auto-update status based on progress
            if (finishPercentage == 100) {
                ticket.setStatus(TicketStatus.CLOSED);
            } else if (finishPercentage > 0 && ticket.getStatus() == TicketStatus.OPEN) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            }
            
            Ticket updatedTicket = ticketRepository.save(ticket);
            
            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_PROGRESS_UPDATED", updatedTicket);
            
            return updatedTicket;
        });
    }

    // Legacy method without access control - kept for backward compatibility
    public Optional<Ticket> updateTicketProgress(Long ticketId, Integer finishPercentage) {
        return ticketRepository.findById(ticketId).map(ticket -> {
            ticket.setFinishPercentage(finishPercentage);
            
            // Auto-update status based on progress
            if (finishPercentage == 100) {
                ticket.setStatus(TicketStatus.CLOSED);
            } else if (finishPercentage > 0 && ticket.getStatus() == TicketStatus.OPEN) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            }
            
            Ticket updatedTicket = ticketRepository.save(ticket);
            
            // Send Kafka event (disabled for now due to serialization issues)
            // publishTicketEvent("TICKET_PROGRESS_UPDATED", updatedTicket);
            
            return updatedTicket;
        });
    }

    public Long getTicketCountByStatus(TicketStatus status) {
        return ticketRepository.countByStatus(status);
    }

    public Long getOpenTicketsByAssignee(Long assigneeId) {
        Optional<User> user = userRepository.findById(assigneeId);
        return user.map(ticketRepository::countOpenTicketsByAssignee).orElse(0L);
    }

    public Long getTicketCountByProject(Long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.map(ticketRepository::countByProject).orElse(0L);
    }

    public Double getProjectProgress(Long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.map(ticketRepository::getAverageProgressByProject).orElse(0.0);
    }

    private void publishTicketEvent(String eventType, Ticket ticket) {
        try {
            TicketEvent event = new TicketEvent(eventType, ticket);
            kafkaTemplate.send(TICKET_TOPIC, event);
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Failed to publish ticket event: " + e.getMessage());
        }
    }

    // Inner class for Kafka events
    public static class TicketEvent {
        private String eventType;
        private Long ticketId;
        private String ticketTitle;
        private String projectName;
        private long timestamp;

        public TicketEvent(String eventType, Ticket ticket) {
            this.eventType = eventType;
            this.ticketId = ticket.getId();
            this.ticketTitle = ticket.getTitle();
            this.projectName = ticket.getProject() != null ? ticket.getProject().getName() : null;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public Long getTicketId() {
            return ticketId;
        }

        public void setTicketId(Long ticketId) {
            this.ticketId = ticketId;
        }

        public String getTicketTitle() {
            return ticketTitle;
        }

        public void setTicketTitle(String ticketTitle) {
            this.ticketTitle = ticketTitle;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
} 