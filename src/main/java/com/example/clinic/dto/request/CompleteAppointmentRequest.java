package com.example.clinic.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteAppointmentRequest {

    @NotBlank(message = "Diagnosis is required")
    @Size(max = 4000, message = "Diagnosis must not exceed 4000 characters")
    private String diagnosis;

    @Size(max = 4000, message = "Doctor note must not exceed 4000 characters")
    private String doctorNote;

    @NotNull(message = "Medications list cannot be null")
    @Size(max = 50, message = "Cannot prescribe more than 50 medications")
    private List<@NotNull @Valid PrescribedMedicationRequest> medications;
}
