package com.microservices.hrms.repository;

import com.microservices.hrms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // Find attendance by employee and date
    @Query("SELECT a FROM Attendance a WHERE a.employee.authUserId = :authUserId AND a.attendanceDate = :date")
    Optional<Attendance> findByEmployeeAndDate(@Param("authUserId") Long authUserId, 
                                             @Param("date") LocalDate date);
    
    // Get employee's attendance in date range
    @Query("SELECT a FROM Attendance a WHERE a.employee.authUserId = :authUserId AND " +
           "a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findByEmployeeAndDateRange(@Param("authUserId") Long authUserId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
    
    // Get today's attendance for all employees (attendance board)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date " +
           "ORDER BY a.checkInTime ASC")
    List<Attendance> findTodaysAttendance(@Param("date") LocalDate date);
    
    // Get attendance in date range (for reports)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate " +
           "ORDER BY a.attendanceDate DESC, a.employee.name ASC")
    List<Attendance> findAttendanceInDateRange(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
    
    // Get modified attendance records pending approval
    @Query("SELECT a FROM Attendance a WHERE a.isModified = true AND a.approvalStatus = 'PENDING' " +
           "ORDER BY a.modifiedAt ASC")
    List<Attendance> findPendingApprovals();
    
    // Get modified attendance records by specific employee
    @Query("SELECT a FROM Attendance a WHERE a.employee.authUserId = :authUserId AND a.isModified = true " +
           "ORDER BY a.modifiedAt DESC")
    List<Attendance> findModifiedByEmployee(@Param("authUserId") Long authUserId);
    
    // Get all modified records (HR view)
    @Query("SELECT a FROM Attendance a WHERE a.isModified = true " +
           "ORDER BY a.modifiedAt DESC")
    List<Attendance> findAllModifiedRecords();
    
    // Get late arrivals in date range
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate " +
           "AND a.checkInTime > :standardStartTime AND a.status != 'ABSENT' " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findLateArrivals(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     @Param("standardStartTime") java.time.LocalTime standardStartTime);
    
    // Get early departures in date range  
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate " +
           "AND a.checkOutTime < :standardEndTime AND a.checkOutTime IS NOT NULL " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findEarlyDepartures(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("standardEndTime") java.time.LocalTime standardEndTime);
    
    // Get absent employees in date range
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate " +
           "AND a.status = 'ABSENT' " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findAbsentEmployees(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
    
    // Get remote work attendance
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate " +
           "AND a.status = 'REMOTE_WORK' " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findRemoteWorkAttendance(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
    
    // Statistics queries
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.authUserId = :authUserId AND " +
           "a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate AND " +
           "a.status IN ('PRESENT', 'LATE', 'EARLY_DEPARTURE', 'REMOTE_WORK')")
    Long countPresentDays(@Param("authUserId") Long authUserId,
                         @Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.authUserId = :authUserId AND " +
           "a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate AND " +
           "a.status = 'ABSENT'")
    Long countAbsentDays(@Param("authUserId") Long authUserId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.authUserId = :authUserId AND " +
           "a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate AND " +
           "a.checkInTime > :standardStartTime")
    Long countLateDays(@Param("authUserId") Long authUserId,
                      @Param("startDate") LocalDate startDate,
                      @Param("endDate") LocalDate endDate,
                      @Param("standardStartTime") java.time.LocalTime standardStartTime);
    
    @Query("SELECT COALESCE(SUM(a.totalHoursWorked), 0) FROM Attendance a WHERE a.employee.authUserId = :authUserId AND " +
           "a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate")
    BigDecimal sumTotalHoursWorked(@Param("authUserId") Long authUserId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(a.overtimeHours), 0) FROM Attendance a WHERE a.employee.authUserId = :authUserId AND " +
           "a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate")
    BigDecimal sumOvertimeHours(@Param("authUserId") Long authUserId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);
    
    // Team statistics
    @Query("SELECT COUNT(DISTINCT a.employee.id) FROM Attendance a WHERE a.attendanceDate = :date AND " +
           "a.status IN ('PRESENT', 'LATE', 'EARLY_DEPARTURE', 'REMOTE_WORK')")
    Long countPresentEmployeesToday(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(DISTINCT a.employee.id) FROM Attendance a WHERE a.attendanceDate = :date AND " +
           "a.status = 'REMOTE_WORK'")
    Long countRemoteEmployeesToday(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(DISTINCT a.employee.id) FROM Attendance a WHERE a.attendanceDate = :date AND " +
           "a.status = 'ABSENT'")
    Long countAbsentEmployeesToday(@Param("date") LocalDate date);
    
    // Department-wise attendance (if needed later)
    @Query("SELECT a FROM Attendance a WHERE a.employee.department.id = :departmentId AND " +
           "a.attendanceDate >= :startDate AND a.attendanceDate <= :endDate " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findByDepartmentAndDateRange(@Param("departmentId") Long departmentId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
    
    // Check if employee has attendance record for specific date
    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.employee.authUserId = :authUserId AND a.attendanceDate = :date")
    boolean existsByEmployeeAndDate(@Param("authUserId") Long authUserId, @Param("date") LocalDate date);
    
    // Find employees who haven't checked out yet today
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND " +
           "a.checkInTime IS NOT NULL AND a.checkOutTime IS NULL " +
           "ORDER BY a.checkInTime ASC")
    List<Attendance> findEmployeesStillWorking(@Param("date") LocalDate date);
} 