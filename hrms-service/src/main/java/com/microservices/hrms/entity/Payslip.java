package com.microservices.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payslips")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;
    
    @Column(name = "pay_period_start", nullable = false)
    private LocalDate payPeriodStart;
    
    @Column(name = "pay_period_end", nullable = false)
    private LocalDate payPeriodEnd;
    
    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;
    
    @Column(name = "basic_salary", precision = 12, scale = 2, nullable = false)
    private BigDecimal basicSalary;
    
    @Column(name = "gross_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal grossPay;
    
    @Column(name = "net_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal netPay;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "earnings", columnDefinition = "jsonb")
    private Map<String, BigDecimal> earnings;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deductions", columnDefinition = "jsonb")
    private Map<String, BigDecimal> deductions;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "taxes", columnDefinition = "jsonb")
    private Map<String, BigDecimal> taxes;
    
    @Column(name = "total_earnings", precision = 12, scale = 2)
    private BigDecimal totalEarnings;
    
    @Column(name = "total_deductions", precision = 12, scale = 2)
    private BigDecimal totalDeductions;
    
    @Column(name = "total_taxes", precision = 12, scale = 2)
    private BigDecimal totalTaxes;
    
    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "USD";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private PayslipStatus status = PayslipStatus.GENERATED;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PayslipStatus {
        GENERATED, SENT, VIEWED, DOWNLOADED
    }
} 