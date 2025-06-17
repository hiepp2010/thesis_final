package com.microservices.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payroll_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "run_name", nullable = false, length = 100)
    private String runName;
    
    @Column(name = "pay_period_start", nullable = false)
    private LocalDate payPeriodStart;
    
    @Column(name = "pay_period_end", nullable = false)
    private LocalDate payPeriodEnd;
    
    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private PayrollStatus status = PayrollStatus.DRAFT;
    
    @Column(name = "total_gross_pay", precision = 15, scale = 2)
    private BigDecimal totalGrossPay;
    
    @Column(name = "total_net_pay", precision = 15, scale = 2)
    private BigDecimal totalNetPay;
    
    @Column(name = "total_deductions", precision = 15, scale = 2)
    private BigDecimal totalDeductions;
    
    @Column(name = "total_taxes", precision = 15, scale = 2)
    private BigDecimal totalTaxes;
    
    @Column(name = "employee_count")
    private Integer employeeCount;
    
    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "USD";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Employee createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payslip> payslips;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PayrollStatus {
        DRAFT, CALCULATED, APPROVED, PROCESSED, COMPLETED
    }
} 