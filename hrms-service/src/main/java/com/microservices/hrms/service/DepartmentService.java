package com.microservices.hrms.service;

import com.microservices.hrms.entity.Department;
import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.repository.DepartmentRepository;
import com.microservices.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DepartmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DepartmentService.class);
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    // Get all active departments
    public List<Department> getAllActiveDepartments() {
        return departmentRepository.findByIsActiveTrue();
    }
    
    // Get department by ID
    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }
    
    // Create new department
    public Department createDepartment(Department department) {
        logger.info("Creating new department: {}", department.getName());
        return departmentRepository.save(department);
    }
    
    // Update department
    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        
        department.setName(departmentDetails.getName());
        department.setDescription(departmentDetails.getDescription());
        department.setBudget(departmentDetails.getBudget());
        department.setLocation(departmentDetails.getLocation());
        
        logger.info("Updated department: {}", department.getName());
        return departmentRepository.save(department);
    }
    
    // Assign department head using auth user ID
    public Department assignDepartmentHead(Long departmentId, Long authUserId) {
        // Validate department exists
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + departmentId));
        
        // Validate employee exists and is active
        Optional<Employee> employeeOpt = employeeRepository.findByAuthUserId(authUserId);
        if (employeeOpt.isEmpty()) {
            throw new RuntimeException("Employee not found with auth user ID: " + authUserId);
        }
        
        Employee employee = employeeOpt.get();
        if (!employee.isActive()) {
            throw new RuntimeException("Cannot assign inactive employee as department head");
        }
        
        // Check if auth user is already a head of another department
        Optional<Department> existingHeadDept = departmentRepository.findByHeadAuthUserId(authUserId);
        if (existingHeadDept.isPresent() && !existingHeadDept.get().getId().equals(departmentId)) {
            throw new RuntimeException("User is already head of department: " + existingHeadDept.get().getName());
        }
        
        // Assign the head
        Long previousHeadId = department.getHeadAuthUserId();
        department.setHeadAuthUserId(authUserId);
        
        // Ensure the employee is also assigned to this department
        if (employee.getDepartment() == null || !employee.getDepartment().getId().equals(departmentId)) {
            employee.setDepartment(department);
            employeeRepository.save(employee);
            logger.info("Assigned employee {} to department {} as part of head assignment", employee.getFullName(), department.getName());
        }
        
        Department savedDepartment = departmentRepository.save(department);
        
        logger.info("Assigned {} (auth user ID: {}) as head of department: {}. Previous head auth user ID: {}", 
                   employee.getFullName(), authUserId, department.getName(), previousHeadId);
        
        return savedDepartment;
    }
    
    // Remove department head
    public Department removeDepartmentHead(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + departmentId));
        
        Long previousHeadId = department.getHeadAuthUserId();
        department.setHeadAuthUserId(null);
        
        Department savedDepartment = departmentRepository.save(department);
        
        logger.info("Removed head from department: {}. Previous head auth user ID: {}", 
                   department.getName(), previousHeadId);
        
        return savedDepartment;
    }
    
    // Get department head employee
    public Optional<Employee> getDepartmentHead(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + departmentId));
        
        if (department.getHeadAuthUserId() == null) {
            return Optional.empty();
        }
        
        return employeeRepository.findByAuthUserId(department.getHeadAuthUserId());
    }
    
    // Check if auth user is department head
    public boolean isAuthUserDepartmentHead(Long authUserId) {
        return departmentRepository.isAuthUserDepartmentHead(authUserId);
    }
    
    // Get department managed by auth user
    public Optional<Department> getDepartmentManagedByAuthUser(Long authUserId) {
        return departmentRepository.findByHeadAuthUserId(authUserId);
    }
    
    // Get departments without heads
    public List<Department> getDepartmentsWithoutHeads() {
        return departmentRepository.findDepartmentsWithoutHeads();
    }
    
    // Get department statistics
    public DepartmentStats getDepartmentStats(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + departmentId));
        
        Long employeeCount = departmentRepository.countActiveEmployeesInDepartment(departmentId);
        Optional<Employee> head = getDepartmentHead(departmentId);
        
        return new DepartmentStats(
                department,
                employeeCount,
                head.orElse(null)
        );
    }
    
    // DTO for department statistics
    public static class DepartmentStats {
        private Department department;
        private Long employeeCount;
        private Employee head;
        
        public DepartmentStats(Department department, Long employeeCount, Employee head) {
            this.department = department;
            this.employeeCount = employeeCount;
            this.head = head;
        }
        
        // Getters
        public Department getDepartment() { return department; }
        public Long getEmployeeCount() { return employeeCount; }
        public Employee getHead() { return head; }
        
        // Setters
        public void setDepartment(Department department) { this.department = department; }
        public void setEmployeeCount(Long employeeCount) { this.employeeCount = employeeCount; }
        public void setHead(Employee head) { this.head = head; }
    }
} 