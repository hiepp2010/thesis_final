package com.microservices.hrms.controller;

import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.repository.EmployeeRepository;
import com.microservices.hrms.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/hrms/employees")
public class EmployeeController {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @GetMapping("/me")
    public ResponseEntity<Employee> getCurrentEmployee() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Optional<Employee> employee = employeeRepository.findByAuthUserId(currentUserId);
        return employee.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAllActiveEmployees();
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        // HR can view any employee, employees can only view their own profile
        if (!SecurityUtils.isCurrentUserHR()) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            Optional<Employee> currentEmployee = employeeRepository.findByAuthUserId(currentUserId);
            if (currentEmployee.isEmpty() || !currentEmployee.get().getId().equals(id)) {
                return ResponseEntity.status(403).build();
            }
        }
        
        Optional<Employee> employee = employeeRepository.findById(id);
        return employee.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<Employee>> searchEmployees(@RequestParam String term) {
        List<Employee> employees = employeeRepository.searchEmployees(term);
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<Employee>> getEmployeesByDepartment(@PathVariable Long departmentId) {
        List<Employee> employees = employeeRepository.findActiveEmployeesByDepartment(departmentId);
        return ResponseEntity.ok(employees);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        Employee savedEmployee = employeeRepository.save(employee);
        return ResponseEntity.ok(savedEmployee);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        employee.setId(id);
        Employee updatedEmployee = employeeRepository.save(employee);
        return ResponseEntity.ok(updatedEmployee);
    }
    
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("HRMS Service is running");
    }
} 