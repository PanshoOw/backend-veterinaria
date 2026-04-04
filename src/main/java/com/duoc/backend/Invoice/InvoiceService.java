package com.duoc.backend.Invoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.duoc.backend.Appointment.Appointment;
import com.duoc.backend.Appointment.AppointmentRepository;
import com.duoc.backend.Care.Care;
import com.duoc.backend.Care.CareRepository;
import com.duoc.backend.Medication.Medication;
import com.duoc.backend.Medication.MedicationRepository;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private CareRepository careRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public Iterable<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id).orElse(null);
    }

    public Invoice generateInvoice(InvoiceRequest request) {
        if (request.getAppointmentId() == null) {
            throw new IllegalArgumentException("Debe indicar la visita a facturar.");
        }

        if (request.getPatientName() == null || request.getPatientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el nombre del paciente.");
        }

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("La visita indicada no existe."));

        List<Long> careIds = request.getCareIds() != null ? request.getCareIds() : Collections.emptyList();
        List<Long> medicationIds = request.getMedicationIds() != null ? request.getMedicationIds() : Collections.emptyList();

        List<Care> validCares = careIds.isEmpty()
                ? new ArrayList<>()
                : (List<Care>) careRepository.findAllById(careIds);

        if (validCares.size() != careIds.size()) {
            throw new IllegalArgumentException("Algunos servicios no existen en la base de datos.");
        }

        List<Medication> validMedications = medicationIds.isEmpty()
                ? new ArrayList<>()
                : (List<Medication>) medicationRepository.findAllById(medicationIds);

        if (validMedications.size() != medicationIds.size()) {
            throw new IllegalArgumentException("Algunos medicamentos no existen en la base de datos.");
        }

        List<AdditionalCharge> charges = new ArrayList<>();
        double totalAdditionalCharges = 0.0;

        if (request.getAdditionalCharges() != null) {
            for (AdditionalChargeRequest chargeRequest : request.getAdditionalCharges()) {
                if (chargeRequest.getDescription() == null || chargeRequest.getDescription().trim().isEmpty()) {
                    throw new IllegalArgumentException("Cada cargo adicional debe tener descripción.");
                }

                if (chargeRequest.getAmount() == null || chargeRequest.getAmount() < 0) {
                    throw new IllegalArgumentException("Cada cargo adicional debe tener un monto válido.");
                }

                AdditionalCharge charge = new AdditionalCharge();
                charge.setDescription(chargeRequest.getDescription().trim());
                charge.setAmount(chargeRequest.getAmount());

                charges.add(charge);
                totalAdditionalCharges += charge.getAmount();
            }
        }

        if (validCares.isEmpty() && validMedications.isEmpty() && charges.isEmpty()) {
            throw new IllegalArgumentException("La factura debe contener al menos un servicio, medicamento o cargo adicional.");
        }

        double totalCareCost = validCares.stream()
                .mapToDouble(Care::getCost)
                .sum();

        double totalMedicationCost = validMedications.stream()
                .mapToDouble(Medication::getCost)
                .sum();

        double total = totalCareCost + totalMedicationCost + totalAdditionalCharges;

        Invoice invoice = new Invoice();
        invoice.setPatientName(request.getPatientName().trim());
        invoice.setAppointment(appointment);
        invoice.setDate(appointment.getDate());
        invoice.setTime(appointment.getTime());
        invoice.setCares(validCares);
        invoice.setMedications(validMedications);
        invoice.setTotalCost(total);

        for (AdditionalCharge charge : charges) {
            charge.setInvoice(invoice);
        }

        invoice.setAdditionalCharges(charges);

        return invoiceRepository.save(invoice);
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }
}