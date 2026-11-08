package com.example.clinic.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentMedicationResponse {

    private Long id;
    private Instant createdAt;
    private long version;
    private Long appointmentId;
    private Long medicationId;
    private String medicineName;
    private String strength;
    private String unit;
    private int quantity;
    private String dosage;
    private String frequency;
    private int durationDays;
    private String instruction;
}
