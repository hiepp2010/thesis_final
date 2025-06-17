package com.microservices.hrms.repository;

import com.microservices.hrms.entity.OvertimeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest, Long> {
    
    // For overtime board - get all approved requests within a date range
    @Query("SELECT o FROM OvertimeRequest o WHERE o.status = 'APPROVED' AND " +
           "o.overtimeDate >= :startDate AND o.overtimeDate <= :endDate " +
           "ORDER BY o.overtimeDate ASC, o.startTime ASC")
    List<OvertimeRequest> findApprovedRequestsInDateRange(@Param("startDate") LocalDate startDate, 
                                                         @Param("endDate") LocalDate endDate);
    
    // Get all approved requests (for full overtime board view)
    @Query("SELECT o FROM OvertimeRequest o WHERE o.status = 'APPROVED' " +
           "ORDER BY o.overtimeDate ASC, o.startTime ASC")
    List<OvertimeRequest> findAllApprovedRequests();
    
    // Get today's overtime (people working overtime today)
    @Query("SELECT o FROM OvertimeRequest o WHERE o.status = 'APPROVED' AND " +
           "o.overtimeDate = :today " +
           "ORDER BY o.startTime ASC")
    List<OvertimeRequest> findTodaysOvertime(@Param("today") LocalDate today);
    
    // Get upcoming overtime (approved future overtime)
    @Query("SELECT o FROM OvertimeRequest o WHERE o.status = 'APPROVED' AND " +
           "o.overtimeDate > :today " +
           "ORDER BY o.overtimeDate ASC")
    List<OvertimeRequest> findUpcomingOvertime(@Param("today") LocalDate today);
    
    // Get employee's own requests
    @Query("SELECT o FROM OvertimeRequest o WHERE o.employee.authUserId = :authUserId " +
           "ORDER BY o.createdAt DESC")
    List<OvertimeRequest> findByEmployeeAuthUserId(@Param("authUserId") Long authUserId);
    
    // Get pending requests for approval (managers/HR)
    @Query("SELECT o FROM OvertimeRequest o WHERE o.status = 'PENDING' " +
           "ORDER BY o.overtimeDate ASC, o.createdAt ASC")
    List<OvertimeRequest> findPendingRequests();
    
    // Get urgent pending requests
    @Query("SELECT o FROM OvertimeRequest o WHERE o.status = 'PENDING' AND o.isUrgent = true " +
           "ORDER BY o.overtimeDate ASC, o.createdAt ASC")
    List<OvertimeRequest> findUrgentPendingRequests();
    
    // Get requests by status
    List<OvertimeRequest> findByStatusOrderByCreatedAtDesc(OvertimeRequest.RequestStatus status);
    
    // Get employee's requests by status
    @Query("SELECT o FROM OvertimeRequest o WHERE o.employee.authUserId = :authUserId AND o.status = :status " +
           "ORDER BY o.createdAt DESC")
    List<OvertimeRequest> findByEmployeeAuthUserIdAndStatus(@Param("authUserId") Long authUserId, 
                                                           @Param("status") OvertimeRequest.RequestStatus status);
    
    // Check for overlapping overtime requests on same date
    @Query("SELECT o FROM OvertimeRequest o WHERE o.employee.authUserId = :authUserId AND " +
           "o.status IN ('PENDING', 'APPROVED') AND " +
           "o.overtimeDate = :overtimeDate AND " +
           "((o.startTime < :endTime AND o.endTime > :startTime) OR " +
           "(o.startTime IS NULL OR o.endTime IS NULL))")
    List<OvertimeRequest> findOverlappingRequests(@Param("authUserId") Long authUserId,
                                                 @Param("overtimeDate") LocalDate overtimeDate,
                                                 @Param("startTime") java.time.LocalTime startTime,
                                                 @Param("endTime") java.time.LocalTime endTime);
    
    // Statistics queries
    @Query("SELECT COUNT(o) FROM OvertimeRequest o WHERE o.employee.authUserId = :authUserId AND " +
           "o.status = 'APPROVED' AND YEAR(o.overtimeDate) = :year")
    Long countApprovedRequestsByEmployeeAndYear(@Param("authUserId") Long authUserId, 
                                               @Param("year") int year);
    
    @Query("SELECT COALESCE(SUM(o.hoursRequested), 0) FROM OvertimeRequest o WHERE o.employee.authUserId = :authUserId AND " +
           "o.status = 'APPROVED' AND YEAR(o.overtimeDate) = :year")
    BigDecimal sumApprovedHoursByEmployeeAndYear(@Param("authUserId") Long authUserId, 
                                                @Param("year") int year);
    
    @Query("SELECT COALESCE(SUM(o.hoursRequested), 0) FROM OvertimeRequest o WHERE " +
           "o.status = 'APPROVED' AND MONTH(o.overtimeDate) = :month AND YEAR(o.overtimeDate) = :year")
    BigDecimal sumApprovedHoursByMonth(@Param("month") int month, @Param("year") int year);
    
    // Weekend overtime
    @Query("SELECT o FROM OvertimeRequest o WHERE o.status = 'APPROVED' AND " +
           "FUNCTION('DAYOFWEEK', o.overtimeDate) IN (1, 7) AND " + // Sunday=1, Saturday=7 in MySQL
           "o.overtimeDate >= :startDate AND o.overtimeDate <= :endDate " +
           "ORDER BY o.overtimeDate ASC")
    List<OvertimeRequest> findWeekendOvertimeInDateRange(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
    
    // Emergency overtime
    @Query("SELECT o FROM OvertimeRequest o WHERE o.status = 'APPROVED' AND " +
           "o.overtimeType = 'EMERGENCY' AND " +
           "o.overtimeDate >= :startDate AND o.overtimeDate <= :endDate " +
           "ORDER BY o.overtimeDate ASC")
    List<OvertimeRequest> findEmergencyOvertimeInDateRange(@Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);
    
    // Cost calculation
    @Query("SELECT COALESCE(SUM(o.estimatedRate * o.hoursRequested), 0) FROM OvertimeRequest o WHERE " +
           "o.status = 'APPROVED' AND o.overtimeDate >= :startDate AND o.overtimeDate <= :endDate")
    BigDecimal calculateEstimatedCostInDateRange(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
} 