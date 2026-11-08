package com.example.clinic.mapper;

import com.example.clinic.dto.response.*;
import com.example.clinic.model.*;
import org.springframework.stereotype.Component;

@Component
public class ClinicMapper {

    public ExamSessionResponse toExamSessionResponse(ExamSession session) {
        return toExamSessionResponse(session, null);
    }

    public ExamSessionResponse toExamSessionResponse(ExamSession session, Integer availableSlots) {
        if (session == null) {
            return null;
        }
        return ExamSessionResponse.builder()
                .id(session.getId())
                .createdAt(session.getCreatedAt())
                .version(session.getVersion())
                .doctorId(session.getDoctorId())
                .examDate(session.getExamDate())
                .shift(session.getShift())
                .examinationFee(session.getExaminationFee())
                .maxPatients(session.getMaxPatients())
                .lastQueueNumber(session.getLastQueueNumber())
                .status(session.getStatus())
                .availableSlots(availableSlots)
                .build();
    }


    public AppointmentResponse toAppointmentResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .createdAt(appointment.getCreatedAt())
                .version(appointment.getVersion())
                .sessionId(appointment.getSessionId())
                .patientId(appointment.getPatientId())
                .queueNumber(appointment.getQueueNumber())
                .examinationFee(appointment.getExaminationFee())
                .status(appointment.getStatus())
                .expiresAt(appointment.getExpiresAt())
                .examinationInvoiceId(appointment.getExaminationInvoiceId())
                .examinationPaidAt(appointment.getExaminationPaidAt())
                .symptoms(appointment.getSymptoms())
                .diagnosis(appointment.getDiagnosis())
                .doctorNote(appointment.getDoctorNote())
                .completedAt(appointment.getCompletedAt())
                .build();
    }

    public MedicalServiceResponse toMedicalServiceResponse(MedicalService service) {
        if (service == null) {
            return null;
        }
        return MedicalServiceResponse.builder()
                .id(service.getId())
                .createdAt(service.getCreatedAt())
                .version(service.getVersion())
                .name(service.getName())
                .serviceType(service.getServiceType())
                .price(service.getPrice())
                .roomNumber(service.getRoomNumber())
                .description(service.getDescription())
                .active(service.isActive())
                .build();
    }

    public MedicationResponse toMedicationResponse(Medication medication) {
        if (medication == null) {
            return null;
        }
        return MedicationResponse.builder()
                .id(medication.getId())
                .createdAt(medication.getCreatedAt())
                .version(medication.getVersion())
                .name(medication.getName())
                .strength(medication.getStrength())
                .dosageForm(medication.getDosageForm())
                .unit(medication.getUnit())
                .description(medication.getDescription())
                .active(medication.isActive())
                .build();
    }

    public AppointmentServiceResponse toAppointmentServiceResponse(AppointmentService item) {
        if (item == null) {
            return null;
        }
        return AppointmentServiceResponse.builder()
                .id(item.getId())
                .createdAt(item.getCreatedAt())
                .version(item.getVersion())
                .appointmentId(item.getAppointmentId())
                .serviceId(item.getServiceId())
                .invoiceId(item.getInvoiceId())
                .serviceName(item.getServiceName())
                .serviceType(item.getServiceType())
                .unitPrice(item.getUnitPrice())
                .roomNumber(item.getRoomNumber())
                .paidAt(item.getPaidAt())
                .status(item.getStatus())
                .resultUrl(item.getResultUrl())
                .note(item.getNote())
                .resultAddedBy(item.getResultAddedBy())
                .completedAt(item.getCompletedAt())
                .build();
    }

    public AppointmentMedicationResponse toAppointmentMedicationResponse(AppointmentMedication line) {
        if (line == null) {
            return null;
        }
        return AppointmentMedicationResponse.builder()
                .id(line.getId())
                .createdAt(line.getCreatedAt())
                .version(line.getVersion())
                .appointmentId(line.getAppointmentId())
                .medicationId(line.getMedicationId())
                .medicineName(line.getMedicineName())
                .strength(line.getStrength())
                .unit(line.getUnit())
                .quantity(line.getQuantity())
                .dosage(line.getDosage())
                .frequency(line.getFrequency())
                .durationDays(line.getDurationDays())
                .instruction(line.getInstruction())
                .build();
    }
}
