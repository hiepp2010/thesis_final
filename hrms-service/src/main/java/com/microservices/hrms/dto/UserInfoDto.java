package com.microservices.hrms.dto;

import com.microservices.hrms.entity.Employee;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class UserInfoDto {
    
    // Public info that all users can see
    @Data
    @Builder
    public static class PublicInfo {
        private Long authUserId;
        private String employeeId;
        private String name;
        private String email;
        private String department;
        private String jobPosition;
        private String avatarUrl;
        private Employee.EmploymentType employmentType;
        private Employee.WorkLocation workLocation;
        private Employee.EmployeeStatus status;
        private List<String> skills;
        private String bio;
    }
    
    // Full info that managers can see for their reports and HR can see for everyone
    @Data
    @Builder
    public static class FullInfo {
        private Long authUserId;
        private String employeeId;
        private String name;
        private String email;
        private String phone;
        private String department;
        private String jobPosition;
        private String managerName;
        private LocalDate hireDate;
        private Employee.EmploymentType employmentType;
        private Employee.WorkLocation workLocation;
        private Employee.EmployeeStatus status;
        private String avatarUrl;
        private List<String> skills;
        private String bio;
        private LocalDate terminationDate;
        private String terminationReason;
        
        // Additional info for managers/HR
        private PersonalInfoSummary personalInfo;
        private AttendanceSummary attendanceSummary;
        private boolean canViewSalary;
        private SalaryInfo salaryInfo; // Only if canViewSalary is true
    }
    
    @Data
    @Builder
    public static class PersonalInfoSummary {
        private LocalDate dateOfBirth;
        private String nationality;
        private String address;
        private String emergencyContactName;
        private String emergencyContactPhone;
    }
    
    @Data
    @Builder
    public static class AttendanceSummary {
        private int presentDaysThisMonth;
        private int totalWorkingDaysThisMonth;
        private double attendancePercentage;
    }
    
    @Data
    @Builder
    public static class SalaryInfo {
        private String salary;
        private String currency;
    }
} 