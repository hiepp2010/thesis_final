package com.microservices.hrms.service;

import com.microservices.hrms.entity.Department;
import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.repository.DepartmentRepository;
import com.microservices.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ManagerService.class);
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    /**
     * Get all employees directly reporting to a manager
     */
    public List<Employee> getDirectReports(Long managerAuthUserId) {
        logger.info("Getting direct reports for manager with auth user ID: {}", managerAuthUserId);
        return employeeRepository.findDirectReportsByManagerAuthUserId(managerAuthUserId);
    }
    
    /**
     * Get all employees in departments managed by a department head
     */
    public List<Employee> getDepartmentMembers(Long departmentHeadAuthUserId) {
        logger.info("Getting department members for head with auth user ID: {}", departmentHeadAuthUserId);
        return employeeRepository.findEmployeesByDepartmentHead(departmentHeadAuthUserId);
    }
    
    /**
     * Get ALL employees that a manager can manage (direct reports + department members)
     */
    public List<Employee> getAllManagedEmployees(Long managerAuthUserId) {
        logger.info("Getting all managed employees for manager with auth user ID: {}", managerAuthUserId);
        return employeeRepository.findAllManagedEmployeesByAuthUserId(managerAuthUserId);
    }
    
    /**
     * Get comprehensive management summary for a manager
     */
    public ManagerSummary getManagerSummary(Long managerAuthUserId) {
        // Get different types of managed employees
        List<Employee> directReports = getDirectReports(managerAuthUserId);
        List<Employee> departmentMembers = getDepartmentMembers(managerAuthUserId);
        List<Employee> allManaged = getAllManagedEmployees(managerAuthUserId);
        
        // Get managed department info
        Optional<Department> managedDepartment = departmentRepository.findByHeadAuthUserId(managerAuthUserId);
        
        // Get manager's own employee record
        Optional<Employee> managerEmployee = employeeRepository.findByAuthUserId(managerAuthUserId);
        
        return ManagerSummary.builder()
                .managerAuthUserId(managerAuthUserId)
                .managerEmployee(managerEmployee.orElse(null))
                .managedDepartment(managedDepartment.orElse(null))
                .directReports(directReports)
                .departmentMembers(departmentMembers)
                .allManagedEmployees(allManaged)
                .directReportsCount(directReports.size())
                .departmentMembersCount(departmentMembers.size())
                .totalManagedCount(allManaged.size())
                .isDepartmentHead(managedDepartment.isPresent())
                .hasDirectReports(!directReports.isEmpty())
                .build();
    }
    
    /**
     * Check if a manager can manage a specific employee
     */
    public boolean canManagerManageEmployee(Long managerAuthUserId, Long employeeAuthUserId) {
        List<Employee> managedEmployees = getAllManagedEmployees(managerAuthUserId);
        return managedEmployees.stream()
                .anyMatch(emp -> emp.getAuthUserId().equals(employeeAuthUserId));
    }
    
    /**
     * Assign a direct manager to an employee
     */
    public Employee assignDirectManager(Long employeeId, Long managerAuthUserId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        
        Employee manager = employeeRepository.findByAuthUserId(managerAuthUserId)
                .orElseThrow(() -> new RuntimeException("Manager not found with auth user ID: " + managerAuthUserId));
        
        employee.setManager(manager);
        Employee savedEmployee = employeeRepository.save(employee);
        
        logger.info("Assigned {} as direct manager to employee {}", 
                   manager.getFullName(), employee.getFullName());
        
        return savedEmployee;
    }
    
    /**
     * Remove direct manager from an employee
     */
    public Employee removeDirectManager(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        
        String previousManager = employee.getManager() != null ? employee.getManager().getFullName() : "None";
        employee.setManager(null);
        Employee savedEmployee = employeeRepository.save(employee);
        
        logger.info("Removed direct manager ({}) from employee {}", 
                   previousManager, employee.getFullName());
        
        return savedEmployee;
    }
    
    /**
     * Get organizational chart data for a manager
     */
    public Map<String, Object> getOrganizationalChart(Long managerAuthUserId) {
        ManagerSummary summary = getManagerSummary(managerAuthUserId);
        
        Map<String, Object> chart = new HashMap<>();
        chart.put("manager", summary.getManagerEmployee());
        chart.put("managedDepartment", summary.getManagedDepartment());
        
        // Group employees by management type
        Map<String, List<Employee>> managementStructure = new HashMap<>();
        managementStructure.put("directReports", summary.getDirectReports());
        managementStructure.put("departmentMembers", summary.getDepartmentMembers());
        
        chart.put("managementStructure", managementStructure);
        chart.put("summary", Map.of(
            "totalManagedEmployees", summary.getTotalManagedCount(),
            "directReportsCount", summary.getDirectReportsCount(),
            "departmentMembersCount", summary.getDepartmentMembersCount(),
            "isDepartmentHead", summary.isDepartmentHead(),
            "hasDirectReports", summary.isHasDirectReports()
        ));
        
        return chart;
    }
    
    // DTO for manager summary
    @lombok.Data
    @lombok.Builder
    public static class ManagerSummary {
        private Long managerAuthUserId;
        private Employee managerEmployee;
        private Department managedDepartment;
        private List<Employee> directReports;
        private List<Employee> departmentMembers;
        private List<Employee> allManagedEmployees;
        private int directReportsCount;
        private int departmentMembersCount;
        private int totalManagedCount;
        private boolean isDepartmentHead;
        private boolean hasDirectReports;
    }
} 