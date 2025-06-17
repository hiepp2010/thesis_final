package com.microservices.hrms.repository;

import com.microservices.hrms.entity.OffRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OffRequestRepository extends JpaRepository<OffRequest, Long> {
    
    // For time board - get all approved requests within a date range
    @Query("SELECT o FROM OffRequest o WHERE o.status = 'APPROVED' AND " +
           "((o.startDate <= :endDate AND o.endDate >= :startDate)) " +
           "ORDER BY o.startDate ASC")
    List<OffRequest> findApprovedRequestsInDateRange(@Param("startDate") LocalDate startDate, 
                                                    @Param("endDate") LocalDate endDate);
    
    // Get all approved requests (for full time board view)
    @Query("SELECT o FROM OffRequest o WHERE o.status = 'APPROVED' " +
           "ORDER BY o.startDate ASC")
    List<OffRequest> findAllApprovedRequests();
    
    // Get current active leave (people currently on leave)
    @Query("SELECT o FROM OffRequest o WHERE o.status = 'APPROVED' AND " +
           "o.startDate <= :today AND o.endDate >= :today " +
           "ORDER BY o.endDate ASC")
    List<OffRequest> findCurrentActiveLeave(@Param("today") LocalDate today);
    
    // Get upcoming leave (approved future leave)
    @Query("SELECT o FROM OffRequest o WHERE o.status = 'APPROVED' AND " +
           "o.startDate > :today " +
           "ORDER BY o.startDate ASC")
    List<OffRequest> findUpcomingLeave(@Param("today") LocalDate today);
    
    // Get employee's own requests
    @Query("SELECT o FROM OffRequest o WHERE o.employee.authUserId = :authUserId " +
           "ORDER BY o.createdAt DESC")
    List<OffRequest> findByEmployeeAuthUserId(@Param("authUserId") Long authUserId);
    
    // Get pending requests for approval (managers/HR)
    @Query("SELECT o FROM OffRequest o WHERE o.status = 'PENDING' " +
           "ORDER BY o.createdAt ASC")
    List<OffRequest> findPendingRequests();
    
    // Get requests by status
    List<OffRequest> findByStatusOrderByCreatedAtDesc(OffRequest.RequestStatus status);
    
    // Get employee's requests by status
    @Query("SELECT o FROM OffRequest o WHERE o.employee.authUserId = :authUserId AND o.status = :status " +
           "ORDER BY o.createdAt DESC")
    List<OffRequest> findByEmployeeAuthUserIdAndStatus(@Param("authUserId") Long authUserId, 
                                                       @Param("status") OffRequest.RequestStatus status);
    
    // Check for overlapping requests (to prevent double booking)
    @Query("SELECT o FROM OffRequest o WHERE o.employee.authUserId = :authUserId AND " +
           "o.status IN ('PENDING', 'APPROVED') AND " +
           "((o.startDate <= :endDate AND o.endDate >= :startDate))")
    List<OffRequest> findOverlappingRequests(@Param("authUserId") Long authUserId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    // Statistics queries
    @Query("SELECT COUNT(o) FROM OffRequest o WHERE o.employee.authUserId = :authUserId AND " +
           "o.status = 'APPROVED' AND YEAR(o.startDate) = :year")
    Long countApprovedRequestsByEmployeeAndYear(@Param("authUserId") Long authUserId, 
                                               @Param("year") int year);
    
    @Query("SELECT COALESCE(SUM(o.daysRequested), 0) FROM OffRequest o WHERE o.employee.authUserId = :authUserId AND " +
           "o.status = 'APPROVED' AND YEAR(o.startDate) = :year")
    Long sumApprovedDaysByEmployeeAndYear(@Param("authUserId") Long authUserId, 
                                         @Param("year") int year);
} 