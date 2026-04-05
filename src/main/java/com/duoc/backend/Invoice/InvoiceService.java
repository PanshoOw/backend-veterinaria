package com.duoc.backend.Invoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.duoc.backend.Appointment.Appointment;
import com.duoc.backend.Appointment.AppointmentRepository;
import com.duoc.backend.Care.Care;
import com.duoc.backend.Care.CareRepository;
import com.duoc.backend.Medication.Medication;
import com.duoc.backend.Medication.MedicationRepository;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final MedicationRepository medicationRepository;
    private final CareRepository careRepository;
    private final AppointmentRepository appointmentRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            MedicationRepository medicationRepository,
            CareRepository careRepository,
            AppointmentRepository appointmentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.medicationRepository = medicationRepository;
        this.careRepository = careRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Iterable<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id).orElse(null);
    }

    public Invoice generateInvoice(InvoiceRequest request) {
        validateRequest(request);

        Appointment appointment = findAppointment(request.getAppointmentId());
        List<Care> validCares = findValidCares(request.getCareIds());
        List<Medication> validMedications = findValidMedications(request.getMedicationIds());
        List<AdditionalCharge> charges = buildAdditionalCharges(request.getAdditionalCharges());

        validateInvoiceHasItems(validCares, validMedications, charges);

        double total = calculateTotal(validCares, validMedications, charges);

        Invoice invoice = buildInvoice(
                request,
                appointment,
                validCares,
                validMedications,
                charges,
                total
        );

        return invoiceRepository.save(invoice);
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    private void validateRequest(InvoiceRequest request) {
        if (request.getAppointmentId() == null) {
            throw new IllegalArgumentException("Debe indicar la visita a facturar.");
        }

        if (request.getPatientName() == null || request.getPatientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el nombre del paciente.");
        }
    }

    private Appointment findAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("La visita indicada no existe."));
    }

    private List<Care> findValidCares(List<Long> careIds) {
        List<Long> ids = careIds != null ? careIds : Collections.emptyList();

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Care> validCares = (List<Care>) careRepository.findAllById(ids);

        if (validCares.size() != ids.size()) {
            throw new IllegalArgumentException("Algunos servicios no existen en la base de datos.");
        }

        return validCares;
    }

    private List<Medication> findValidMedications(List<Long> medicationIds) {
        List<Long> ids = medicationIds != null ? medicationIds : Collections.emptyList();

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Medication> validMedications = (List<Medication>) medicationRepository.findAllById(ids);

        if (validMedications.size() != ids.size()) {
            throw new IllegalArgumentException("Algunos medicamentos no existen en la base de datos.");
        }

        return validMedications;
    }

    private List<AdditionalCharge> buildAdditionalCharges(List<AdditionalChargeRequest> additionalChargesRequest) {
        List<AdditionalCharge> charges = new ArrayList<>();

        if (additionalChargesRequest == null) {
            return charges;
        }

        for (AdditionalChargeRequest chargeRequest : additionalChargesRequest) {
            validateAdditionalCharge(chargeRequest);

            AdditionalCharge charge = new AdditionalCharge();
            charge.setDescription(chargeRequest.getDescription().trim());
            charge.setAmount(chargeRequest.getAmount());

            charges.add(charge);
        }

        return charges;
    }

    private void validateAdditionalCharge(AdditionalChargeRequest chargeRequest) {
        if (chargeRequest.getDescription() == null || chargeRequest.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Cada cargo adicional debe tener descripción.");
        }

        if (chargeRequest.getAmount() == null || chargeRequest.getAmount() < 0) {
            throw new IllegalArgumentException("Cada cargo adicional debe tener un monto válido.");
        }
    }

    private void validateInvoiceHasItems(
            List<Care> validCares,
            List<Medication> validMedications,
            List<AdditionalCharge> charges) {

        if (validCares.isEmpty() && validMedications.isEmpty() && charges.isEmpty()) {
            throw new IllegalArgumentException(
                    "La factura debe contener al menos un servicio, medicamento o cargo adicional."
            );
        }
    }

    private double calculateTotal(
            List<Care> validCares,
            List<Medication> validMedications,
            List<AdditionalCharge> charges) {

        double totalCareCost = validCares.stream()
                .mapToDouble(Care::getCost)
                .sum();

        double totalMedicationCost = validMedications.stream()
                .mapToDouble(Medication::getCost)
                .sum();

        double totalAdditionalCharges = charges.stream()
                .mapToDouble(AdditionalCharge::getAmount)
                .sum();

        return totalCareCost + totalMedicationCost + totalAdditionalCharges;
    }

    private Invoice buildInvoice(
            InvoiceRequest request,
            Appointment appointment,
            List<Care> validCares,
            List<Medication> validMedications,
            List<AdditionalCharge> charges,
            double total) {

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

        return invoice;
    }
}