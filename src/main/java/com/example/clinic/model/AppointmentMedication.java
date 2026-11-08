package com.example.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "appointment_medications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_appointment_medication", columnNames = {"appointment_id", "medication_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppointmentMedication extends BaseEntity {

    @Column(name = "appointment_id", nullable = false)
    Long appointmentId;

    @Column(name = "medication_id", nullable = false)
    Long medicationId;

    @Column(name = "medicine_name", nullable = false, length = 200)
    String medicineName;

    @Column(name = "strength", nullable = false, length = 100)
    String strength;

    @Column(name = "unit", nullable = false, length = 50)
    String unit;

    @Column(name = "quantity", nullable = false)
    int quantity;

    @Column(name = "dosage", nullable = false, length = 200)
    String dosage;

    @Column(name = "frequency", nullable = false, length = 200)
    String frequency;

    @Column(name = "duration_days", nullable = false)
    int durationDays;

    @Column(name = "instruction", length = 2000)
    String instruction;
}
