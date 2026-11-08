package com.example.clinic.model;

import com.example.clinic.util.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "appointments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_session_queue", columnNames = {"session_id", "queue_number"})
        },
        indexes = {
                @Index(name = "idx_appointments_patient", columnList = "patient_id, created_at"),
                @Index(name = "idx_appointments_session_status", columnList = "session_id, status"),
                @Index(name = "idx_appointments_expiry", columnList = "status, expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Appointment extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    Long sessionId;

    @Column(name = "patient_id", nullable = false)
    Long patientId;

    @Column(name = "queue_number")
    Integer queueNumber;

    @Column(name = "examination_fee", nullable = false, precision = 18, scale = 0)
    BigDecimal examinationFee;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    AppointmentStatus status = AppointmentStatus.PENDING_PAYMENT;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "examination_invoice_id", unique = true)
    Long examinationInvoiceId;

    @Column(name = "examination_paid_at")
    Instant examinationPaidAt;

    @Column(name = "symptoms", length = 2000)
    String symptoms;

    @Column(name = "diagnosis", length = 4000)
    String diagnosis;

    @Column(name = "doctor_note", length = 4000)
    String doctorNote;

    @Column(name = "completed_at")
    Instant completedAt;
}
