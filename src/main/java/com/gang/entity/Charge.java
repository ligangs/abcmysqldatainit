package com.gang.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Charge {
    private Long id;
    private Long clinicId;
    private Long employId;
    private Long patientId;
    private BigDecimal amount;
    private LocalDateTime chargeTime;
    // getter setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }
    public Long getEmployId() { return employId; }
    public void setEmployId(Long employId) { this.employId = employId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getChargeTime() { return chargeTime; }
    public void setChargeTime(LocalDateTime chargeTime) { this.chargeTime = chargeTime; }
}