package com.duoc.backend.Invoice;

import java.util.List;

public class InvoiceRequest {

    private Long appointmentId;
    private String patientName;
    private List<Long> careIds;
    private List<Long> medicationIds;
    private List<AdditionalChargeRequest> additionalCharges;

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public List<Long> getCareIds() {
        return careIds;
    }

    public void setCareIds(List<Long> careIds) {
        this.careIds = careIds;
    }

    public List<Long> getMedicationIds() {
        return medicationIds;
    }

    public void setMedicationIds(List<Long> medicationIds) {
        this.medicationIds = medicationIds;
    }

    public List<AdditionalChargeRequest> getAdditionalCharges() {
        return additionalCharges;
    }

    public void setAdditionalCharges(List<AdditionalChargeRequest> additionalCharges) {
        this.additionalCharges = additionalCharges;
    }
}