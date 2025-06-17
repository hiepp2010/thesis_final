package com.microservices.hrms.service;

import com.microservices.hrms.dto.AttendanceDto;
import com.microservices.hrms.entity.Attendance;
import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.repository.AttendanceRepository;
import com.microservices.hrms.repository.EmployeeRepository;
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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);
    private static final LocalTime STANDARD_START_TIME = LocalTime.of(9, 0); // 9:00 AM
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(18, 0);   // 6:00 PM
    private static final int STANDARD_WORK_HOURS = 8;
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Get attendance board - today's attendance visible to everyone
     */
    public List<AttendanceDto.AttendanceBoard> getAttendanceBoard(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<Attendance> attendanceList = attendanceRepository.findTodaysAttendance(targetDate);
        
        return attendanceList.stream()
                .map(this::convertToAttendanceBoardDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get attendance board summary for dashboard
     */
    public AttendanceDto.AttendanceBoardSummary getAttendanceBoardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate monthStart = today.withDayOfMonth(1);
        
        Long presentToday = attendanceRepository.countPresentEmployeesToday(today);
        Long absentToday = attendanceRepository.countAbsentEmployeesToday(today);
        Long remoteToday = attendanceRepository.countRemoteEmployeesToday(today);
        
        List<Attendance> todaysAttendance = attendanceRepository.findTodaysAttendance(today);
        int lateToday = (int) todaysAttendance.stream()
                .filter(a -> a.getCheckInTime() != null && a.getCheckInTime().isAfter(STANDARD_START_TIME))
                .count();
        
        List<Attendance> stillWorking = attendanceRepository.findEmployeesStillWorking(today);
        List<Attendance> pendingApprovals = attendanceRepository.findPendingApprovals();
        
        int modificationsToday = (int) attendanceRepository.findAllModifiedRecords().stream()
                .filter(a -> a.getModifiedAt() != null && a.getModifiedAt().toLocalDate().equals(today))
                .count();
        
        // Calculate attendance rates
        long totalEmployees = employeeRepository.count();
        BigDecimal todayAttendanceRate = totalEmployees > 0 ? 
                new BigDecimal(presentToday).divide(new BigDecimal(totalEmployees), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)) : 
                BigDecimal.ZERO;
        
        return AttendanceDto.AttendanceBoardSummary.builder()
                .presentToday(presentToday.intValue())
                .absentToday(absentToday.intValue())
                .remoteToday(remoteToday.intValue())
                .lateToday(lateToday)
                .stillWorking(stillWorking.size())
                .pendingApprovals(pendingApprovals.size())
                .modificationsToday(modificationsToday)
                .todayAttendanceRate(todayAttendanceRate)
                .build();
    }
    
    /**
     * Check-in for the current user
     */
    public AttendanceDto.UserView checkIn(AttendanceDto.CheckInRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        Employee employee = employeeRepository.findByAuthUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        LocalDate today = LocalDate.now();
        
        // Check if already checked in today
        Optional<Attendance> existingAttendance = attendanceRepository.findByEmployeeAndDate(currentUserId, today);
        if (existingAttendance.isPresent() && existingAttendance.get().getCheckInTime() != null) {
            throw new RuntimeException("You have already checked in today");
        }
        
        LocalTime checkInTime = LocalTime.now();
        Attendance.AttendanceStatus status = determineStatusFromCheckIn(checkInTime);
        
        Attendance attendance;
        if (existingAttendance.isPresent()) {
            // Update existing record
            attendance = existingAttendance.get();
            attendance.setCheckInTime(checkInTime);
            attendance.setStatus(status);
        } else {
            // Create new record
            attendance = Attendance.builder()
                    .employee(employee)
                    .attendanceDate(today)
                    .checkInTime(checkInTime)
                    .status(status)
                    .build();
        }
        
        if (request.getLocation() != null) attendance.setLocation(request.getLocation());
        if (request.getNotes() != null) attendance.setNotes(request.getNotes());
        
        attendance = attendanceRepository.save(attendance);
        
        logger.info("Employee {} checked in at {} with status {}", 
                employee.getName(), checkInTime, status);
        
        return convertToUserViewDto(attendance);
    }
    
    /**
     * Check-out for the current user
     */
    public AttendanceDto.UserView checkOut(AttendanceDto.CheckOutRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeAndDate(currentUserId, today)
                .orElseThrow(() -> new RuntimeException("No check-in record found for today"));
        
        if (attendance.getCheckInTime() == null) {
            throw new RuntimeException("You must check in before checking out");
        }
        
        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("You have already checked out today");
        }
        
        LocalTime checkOutTime = LocalTime.now();
        attendance.setCheckOutTime(checkOutTime);
        
        if (request.getBreakTimeMinutes() != null) {
            attendance.setBreakDurationMinutes(request.getBreakTimeMinutes());
        }
        
        if (request.getNotes() != null) {
            String existingNotes = attendance.getNotes();
            attendance.setNotes(existingNotes != null ? existingNotes + "; " + request.getNotes() : request.getNotes());
        }
        
        // Calculate working hours and update status
        BigDecimal workingHours = calculateWorkingHours(attendance.getCheckInTime(), checkOutTime, 
                attendance.getBreakDurationMinutes());
        attendance.setTotalHours(workingHours);
        
        // Calculate overtime if applicable
        BigDecimal standardHours = new BigDecimal(STANDARD_WORK_HOURS);
        if (workingHours.compareTo(standardHours) > 0) {
            attendance.setOvertimeHours(workingHours.subtract(standardHours));
        }
        
        // Update status based on checkout time
        attendance.setStatus(determineStatusFromCheckOut(attendance.getCheckInTime(), checkOutTime));
        
        attendance = attendanceRepository.save(attendance);
        
        logger.info("Employee {} checked out at {} with {} working hours", 
                attendance.getEmployee().getName(), checkOutTime, workingHours);
        
        return convertToUserViewDto(attendance);
    }
    
    /**
     * Get user's own attendance history
     */
    public List<AttendanceDto.UserView> getMyAttendance(LocalDate startDate, LocalDate endDate) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        LocalDate defaultStartDate = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate defaultEndDate = endDate != null ? endDate : LocalDate.now();
        
        List<Attendance> attendanceList = attendanceRepository.findByEmployeeAndDateRange(
                currentUserId, defaultStartDate, defaultEndDate);
        
        return attendanceList.stream()
                .map(this::convertToUserViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get attendance for HR view (all employees)
     */
    public List<AttendanceDto.HRView> getAttendanceForHR(LocalDate startDate, LocalDate endDate) {
        if (!SecurityUtils.isCurrentUserHR()) {
            throw new RuntimeException("Only HR can access all attendance records");
        }
        
        LocalDate defaultStartDate = startDate != null ? startDate : LocalDate.now().minusWeeks(1);
        LocalDate defaultEndDate = endDate != null ? endDate : LocalDate.now();
        
        List<Attendance> attendanceList = attendanceRepository.findAttendanceInDateRange(
                defaultStartDate, defaultEndDate);
        
        return attendanceList.stream()
                .map(this::convertToHRViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Modify attendance record (HR only)
     */
    public AttendanceDto.HRView modifyAttendance(Long attendanceId, AttendanceDto.ModificationRequest modification) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.isCurrentUserHR()) {
            throw new RuntimeException("Only HR can modify attendance records");
        }
        
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
        
        Employee hrEmployee = employeeRepository.findByAuthUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("HR employee not found"));
        
        // Store original values if not already modified
        if (!attendance.getIsModified()) {
            attendance.setOriginalCheckInTime(attendance.getCheckInTime());
            attendance.setOriginalCheckOutTime(attendance.getCheckOutTime());
            attendance.setOriginalStatus(attendance.getStatus());
        }
        
        // Apply modifications
        if (modification.getCheckInTime() != null) {
            attendance.setCheckInTime(modification.getCheckInTime());
        }
        if (modification.getCheckOutTime() != null) {
            attendance.setCheckOutTime(modification.getCheckOutTime());
        }
        if (modification.getStatus() != null) {
            attendance.setStatus(modification.getStatus());
        }
        if (modification.getLocation() != null) {
            attendance.setLocation(modification.getLocation());
        }
        if (modification.getNotes() != null) {
            attendance.setNotes(modification.getNotes());
        }
        if (modification.getBreakTimeMinutes() != null) {
            attendance.setBreakDurationMinutes(modification.getBreakTimeMinutes());
        }
        if (modification.getOvertimeHours() != null) {
            attendance.setOvertimeHours(modification.getOvertimeHours());
        }
        
        // Recalculate working hours if check-in/out times changed
        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
            BigDecimal workingHours = calculateWorkingHours(attendance.getCheckInTime(), 
                    attendance.getCheckOutTime(), attendance.getBreakDurationMinutes());
            attendance.setTotalHours(workingHours);
        }
        
        // Mark as modified and requiring approval
        attendance.setIsModified(true);
        attendance.setModifiedBy(hrEmployee);
        attendance.setModifiedAt(LocalDateTime.now());
        attendance.setModificationReason(modification.getModificationReason());
        attendance.setApprovalStatus(Attendance.ApprovalStatus.PENDING);
        
        attendance = attendanceRepository.save(attendance);
        
        logger.info("HR {} modified attendance record {} for employee {}", 
                hrEmployee.getName(), attendanceId, attendance.getEmployee().getName());
        
        return convertToHRViewDto(attendance);
    }
    
    /**
     * Get pending approvals for managers
     */
    public List<AttendanceDto.ManagerView> getPendingApprovals() {
        if (!SecurityUtils.isCurrentUserHR() && !SecurityUtils.isCurrentUserAnyManager()) {
            throw new RuntimeException("Only HR and managers can view pending approvals");
        }
        
        List<Attendance> pendingAttendance = attendanceRepository.findPendingApprovals();
        
        return pendingAttendance.stream()
                .map(this::convertToManagerViewDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Approve or reject attendance modification (Manager only)
     */
    public AttendanceDto.ManagerView processApproval(Long attendanceId, AttendanceDto.ApprovalAction action) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.isCurrentUserHR() && !SecurityUtils.isCurrentUserAnyManager()) {
            throw new RuntimeException("Only HR and managers can approve attendance modifications");
        }
        
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
        
        if (!attendance.getIsModified() || attendance.getApprovalStatus() != Attendance.ApprovalStatus.PENDING) {
            throw new RuntimeException("This attendance record doesn't require approval");
        }
        
        Employee approver = employeeRepository.findByAuthUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Approver employee not found"));
        
        attendance.setApprovalStatus(action.getAction());
        attendance.setApprovedBy(approver);
        attendance.setApprovedAt(LocalDateTime.now());
        if (action.getApprovalNotes() != null) {
            // Store approval notes in the notes field since there's no separate field
            String existingNotes = attendance.getNotes();
            String approvalNote = "APPROVAL: " + action.getApprovalNotes();
            attendance.setNotes(existingNotes != null ? existingNotes + "; " + approvalNote : approvalNote);
        }
        
        if (action.getAction() == Attendance.ApprovalStatus.REJECTED) {
            // Restore original values if rejected
            attendance.setCheckInTime(attendance.getOriginalCheckInTime());
            attendance.setCheckOutTime(attendance.getOriginalCheckOutTime());
            attendance.setStatus(attendance.getOriginalStatus());
            attendance.setIsModified(false);
            
            // Recalculate working hours with original times
            if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
                BigDecimal workingHours = calculateWorkingHours(attendance.getCheckInTime(), 
                        attendance.getCheckOutTime(), attendance.getBreakDurationMinutes());
                attendance.setTotalHours(workingHours);
            }
        }
        
        attendance = attendanceRepository.save(attendance);
        
        String actionText = action.getAction() == Attendance.ApprovalStatus.APPROVED ? "approved" : "rejected";
        logger.info("Manager {} {} attendance modification for employee {}", 
                approver.getName(), actionText, attendance.getEmployee().getName());
        
        return convertToManagerViewDto(attendance);
    }
    
    /**
     * Get attendance statistics
     */
    public AttendanceDto.Statistics getAttendanceStatistics(LocalDate startDate, LocalDate endDate) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        LocalDate defaultStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate defaultEndDate = endDate != null ? endDate : LocalDate.now();
        
        Long presentDays = attendanceRepository.countPresentDays(currentUserId, defaultStartDate, defaultEndDate);
        Long absentDays = attendanceRepository.countAbsentDays(currentUserId, defaultStartDate, defaultEndDate);
        Long lateDays = attendanceRepository.countLateDays(currentUserId, defaultStartDate, defaultEndDate, STANDARD_START_TIME);
        
        BigDecimal totalHours = attendanceRepository.sumTotalHoursWorked(currentUserId, defaultStartDate, defaultEndDate);
        BigDecimal overtimeHours = attendanceRepository.sumOvertimeHours(currentUserId, defaultStartDate, defaultEndDate);
        
        long totalDays = ChronoUnit.DAYS.between(defaultStartDate, defaultEndDate) + 1;
        
        BigDecimal attendanceRate = totalDays > 0 ? 
                new BigDecimal(presentDays).divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)) : 
                BigDecimal.ZERO;
        
        BigDecimal punctualityRate = presentDays > 0 ? 
                new BigDecimal(presentDays - lateDays).divide(new BigDecimal(presentDays), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)) : 
                BigDecimal.ZERO;
        
        BigDecimal averageHours = presentDays > 0 ? 
                totalHours.divide(new BigDecimal(presentDays), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        
        List<Attendance> modifiedRecords = attendanceRepository.findModifiedByEmployee(currentUserId);
        long modificationsCount = modifiedRecords.size();
        long pendingApprovalsCount = modifiedRecords.stream()
                .filter(a -> a.getApprovalStatus() == Attendance.ApprovalStatus.PENDING)
                .count();
        
        return AttendanceDto.Statistics.builder()
                .totalDays(totalDays)
                .presentDays(presentDays)
                .absentDays(absentDays)
                .lateDays(lateDays)
                .attendanceRate(attendanceRate)
                .punctualityRate(punctualityRate)
                .totalHoursWorked(totalHours)
                .averageHoursPerDay(averageHours)
                .totalOvertimeHours(overtimeHours)
                .modificationsCount(modificationsCount)
                .pendingApprovalsCount(pendingApprovalsCount)
                .build();
    }
    
    // Helper methods
    private Attendance.AttendanceStatus determineStatusFromCheckIn(LocalTime checkInTime) {
        if (checkInTime.isAfter(STANDARD_START_TIME)) {
            return Attendance.AttendanceStatus.LATE;
        }
        return Attendance.AttendanceStatus.PRESENT;
    }
    
    private Attendance.AttendanceStatus determineStatusFromCheckOut(LocalTime checkInTime, LocalTime checkOutTime) {
        boolean isLate = checkInTime.isAfter(STANDARD_START_TIME);
        boolean isEarly = checkOutTime.isBefore(STANDARD_END_TIME);
        
        if (isLate && isEarly) {
            return Attendance.AttendanceStatus.LATE; // Late takes precedence
        } else if (isEarly) {
            return Attendance.AttendanceStatus.EARLY_DEPARTURE;
        } else if (isLate) {
            return Attendance.AttendanceStatus.LATE;
        }
        return Attendance.AttendanceStatus.PRESENT;
    }
    
    private BigDecimal calculateWorkingHours(LocalTime checkIn, LocalTime checkOut, Integer breakMinutes) {
        if (checkIn == null || checkOut == null) {
            return BigDecimal.ZERO;
        }
        
        long totalMinutes = ChronoUnit.MINUTES.between(checkIn, checkOut);
        if (breakMinutes != null) {
            totalMinutes -= breakMinutes;
        }
        
        return new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
    }
    
    private AttendanceDto.AttendanceBoard convertToAttendanceBoardDto(Attendance attendance) {
        return AttendanceDto.AttendanceBoard.builder()
                .id(attendance.getId())
                .employeeName(attendance.getEmployee().getName())
                .employeeId(attendance.getEmployee().getEmployeeId())
                .authUserId(attendance.getEmployee().getAuthUserId())
                .attendanceDate(attendance.getAttendanceDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .status(attendance.getStatus())
                .statusDisplay(attendance.getStatus().getDisplayName())
                .location(attendance.getLocation())
                .totalHoursWorked(attendance.getTotalHours())
                .isPresent(attendance.isPresent())
                .isLate(attendance.isLate())
                .isStillWorking(attendance.getCheckInTime() != null && attendance.getCheckOutTime() == null)
                .avatarUrl(getAvatarUrl(attendance.getEmployee().getProfilePictureUrl()))
                .department(attendance.getEmployee().getDepartment() != null ? 
                          attendance.getEmployee().getDepartment().getName() : null)
                .build();
    }
    
    private AttendanceDto.UserView convertToUserViewDto(Attendance attendance) {
        LocalDate today = LocalDate.now();
        boolean canCheckIn = attendance.getAttendanceDate().equals(today) && attendance.getCheckInTime() == null;
        boolean canCheckOut = attendance.getAttendanceDate().equals(today) && 
                            attendance.getCheckInTime() != null && attendance.getCheckOutTime() == null;
        boolean canModify = !attendance.getAttendanceDate().isAfter(today) && !attendance.getIsModified();
        
        return AttendanceDto.UserView.builder()
                .id(attendance.getId())
                .attendanceDate(attendance.getAttendanceDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .status(attendance.getStatus())
                .statusDisplay(attendance.getStatus().getDisplayName())
                .location(attendance.getLocation())
                .notes(attendance.getNotes())
                .totalHoursWorked(attendance.getTotalHours())
                .overtimeHours(attendance.getOvertimeHours())
                .breakTimeMinutes(attendance.getBreakDurationMinutes())
                .isPresent(attendance.isPresent())
                .isLate(attendance.isLate())
                .isEarlyDeparture(attendance.isEarlyDeparture())
                .canCheckIn(canCheckIn)
                .canCheckOut(canCheckOut)
                .canModify(canModify)
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .isModified(attendance.getIsModified())
                .modificationReason(attendance.getModificationReason())
                .modifiedByName(attendance.getModifiedBy() != null ? attendance.getModifiedBy().getName() : null)
                .modifiedAt(attendance.getModifiedAt())
                .approvalStatus(attendance.getApprovalStatus())
                .approvalStatusDisplay(attendance.getApprovalStatus() != null ? attendance.getApprovalStatus().getDisplayName() : null)
                .approvedByName(attendance.getApprovedBy() != null ? attendance.getApprovedBy().getName() : null)
                .approvedAt(attendance.getApprovedAt())
                .approvalNotes(attendance.getNotes())
                .originalCheckInTime(attendance.getOriginalCheckInTime())
                .originalCheckOutTime(attendance.getOriginalCheckOutTime())
                .originalStatus(attendance.getOriginalStatus())
                .build();
    }
    
    private AttendanceDto.HRView convertToHRViewDto(Attendance attendance) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = currentMonth.plusMonths(1).minusDays(1);
        
        Long authUserId = attendance.getEmployee().getAuthUserId();
        Long presentDaysThisMonth = attendanceRepository.countPresentDays(authUserId, currentMonth, monthEnd);
        Long absentDaysThisMonth = attendanceRepository.countAbsentDays(authUserId, currentMonth, monthEnd);
        Long lateDaysThisMonth = attendanceRepository.countLateDays(authUserId, currentMonth, monthEnd, STANDARD_START_TIME);
        BigDecimal totalHoursThisMonth = attendanceRepository.sumTotalHoursWorked(authUserId, currentMonth, monthEnd);
        
        return AttendanceDto.HRView.builder()
                .id(attendance.getId())
                .employeeName(attendance.getEmployee().getName())
                .employeeId(attendance.getEmployee().getEmployeeId())
                .authUserId(authUserId)
                .employeeEmail(attendance.getEmployee().getEmail())
                .department(attendance.getEmployee().getDepartment() != null ? 
                          attendance.getEmployee().getDepartment().getName() : null)
                .attendanceDate(attendance.getAttendanceDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .status(attendance.getStatus())
                .statusDisplay(attendance.getStatus().getDisplayName())
                .location(attendance.getLocation())
                .notes(attendance.getNotes())
                .totalHoursWorked(attendance.getTotalHours())
                .overtimeHours(attendance.getOvertimeHours())
                .breakTimeMinutes(attendance.getBreakDurationMinutes())
                .isPresent(attendance.isPresent())
                .isLate(attendance.isLate())
                .isEarlyDeparture(attendance.isEarlyDeparture())
                .canModify(true) // HR can always modify
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .avatarUrl(getAvatarUrl(attendance.getEmployee().getProfilePictureUrl()))
                .isModified(attendance.getIsModified())
                .modificationReason(attendance.getModificationReason())
                .modifiedByName(attendance.getModifiedBy() != null ? attendance.getModifiedBy().getName() : null)
                .modifiedAt(attendance.getModifiedAt())
                .approvalStatus(attendance.getApprovalStatus())
                .approvalStatusDisplay(attendance.getApprovalStatus() != null ? attendance.getApprovalStatus().getDisplayName() : null)
                .approvedByName(attendance.getApprovedBy() != null ? attendance.getApprovedBy().getName() : null)
                .approvedAt(attendance.getApprovedAt())
                .approvalNotes(attendance.getNotes())
                .originalCheckInTime(attendance.getOriginalCheckInTime())
                .originalCheckOutTime(attendance.getOriginalCheckOutTime())
                .originalStatus(attendance.getOriginalStatus())
                .totalPresentDaysThisMonth(presentDaysThisMonth)
                .totalAbsentDaysThisMonth(absentDaysThisMonth)
                .totalLateDaysThisMonth(lateDaysThisMonth)
                .totalHoursThisMonth(totalHoursThisMonth)
                .build();
    }
    
    private AttendanceDto.ManagerView convertToManagerViewDto(Attendance attendance) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = currentMonth.plusMonths(1).minusDays(1);
        
        Long authUserId = attendance.getEmployee().getAuthUserId();
        Long presentDaysThisMonth = attendanceRepository.countPresentDays(authUserId, currentMonth, monthEnd);
        
        List<Attendance> modifications = attendanceRepository.findModifiedByEmployee(authUserId);
        long modificationsThisMonth = modifications.stream()
                .filter(a -> a.getModifiedAt() != null && 
                           a.getModifiedAt().toLocalDate().isAfter(currentMonth.minusDays(1)) &&
                           a.getModifiedAt().toLocalDate().isBefore(monthEnd.plusDays(1)))
                .count();
        
        boolean canApprove = attendance.getApprovalStatus() == Attendance.ApprovalStatus.PENDING;
        boolean canReject = attendance.getApprovalStatus() == Attendance.ApprovalStatus.PENDING;
        
        return AttendanceDto.ManagerView.builder()
                .id(attendance.getId())
                .employeeName(attendance.getEmployee().getName())
                .employeeId(attendance.getEmployee().getEmployeeId())
                .authUserId(authUserId)
                .employeeEmail(attendance.getEmployee().getEmail())
                .department(attendance.getEmployee().getDepartment() != null ? 
                          attendance.getEmployee().getDepartment().getName() : null)
                .attendanceDate(attendance.getAttendanceDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .status(attendance.getStatus())
                .statusDisplay(attendance.getStatus().getDisplayName())
                .location(attendance.getLocation())
                .notes(attendance.getNotes())
                .totalHoursWorked(attendance.getTotalHours())
                .overtimeHours(attendance.getOvertimeHours())
                .breakTimeMinutes(attendance.getBreakDurationMinutes())
                .createdAt(attendance.getCreatedAt())
                .avatarUrl(getAvatarUrl(attendance.getEmployee().getProfilePictureUrl()))
                .isModified(attendance.getIsModified())
                .modificationReason(attendance.getModificationReason())
                .modifiedByName(attendance.getModifiedBy() != null ? attendance.getModifiedBy().getName() : null)
                .modifiedAt(attendance.getModifiedAt())
                .approvalStatus(attendance.getApprovalStatus())
                .approvalStatusDisplay(attendance.getApprovalStatus() != null ? attendance.getApprovalStatus().getDisplayName() : null)
                .canApprove(canApprove)
                .canReject(canReject)
                .originalCheckInTime(attendance.getOriginalCheckInTime())
                .originalCheckOutTime(attendance.getOriginalCheckOutTime())
                .originalStatus(attendance.getOriginalStatus())
                .originalStatusDisplay(attendance.getOriginalStatus() != null ? attendance.getOriginalStatus().getDisplayName() : null)
                .totalPresentDaysThisMonth(presentDaysThisMonth)
                .totalModificationsThisMonth(modificationsThisMonth)
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