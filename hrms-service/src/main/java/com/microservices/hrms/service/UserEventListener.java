package com.microservices.hrms.service;

import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.event.UserRegisteredEvent;
import com.microservices.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    @KafkaListener(topics = "${app.kafka.topic.user-registered:user-registered}", groupId = "hrms-service")
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            logger.info("Received UserRegisteredEvent: {}", event);

            // Check if employee record already exists
            if (employeeRepository.findByAuthUserId(event.getUserId()).isPresent()) {
                logger.info("Employee record already exists for userId: {}", event.getUserId());
                return;
            }

            // Create new employee record
            Employee employee = new Employee();
            employee.setAuthUserId(event.getUserId());
            
            // Use username as default name (user can update later)
            employee.setName(capitalize(event.getUsername()));
            
            employee.setEmail(event.getEmail());
            employee.setHireDate(LocalDate.now());
            employee.setEmploymentType(Employee.EmploymentType.FULL_TIME);
            employee.setWorkLocation(Employee.WorkLocation.OFFICE);
            employee.setStatus(Employee.EmployeeStatus.ACTIVE);
            employee.setCurrency("USD");
            
            // Generate employee ID
            String employeeId = generateEmployeeId();
            employee.setEmployeeId(employeeId);

            Employee savedEmployee = employeeRepository.save(employee);
            logger.info("Created employee record for user {}: Employee ID = {}", 
                       event.getUsername(), savedEmployee.getEmployeeId());

        } catch (Exception e) {
            logger.error("Error processing UserRegisteredEvent: {}", event, e);
            // In a production system, you might want to implement retry logic or dead letter queue
        }
    }

    private String generateEmployeeId() {
        // Simple employee ID generation - in production, you might want a more sophisticated approach
        long count = employeeRepository.count() + 1;
        return String.format("EMP%03d", count);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
} 