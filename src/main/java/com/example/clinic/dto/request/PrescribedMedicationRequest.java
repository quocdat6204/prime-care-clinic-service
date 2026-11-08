package com.example.clinic.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescribedMedicationRequest {

    @NotNull(message = "Medication ID is required")
    private Long medicationId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @NotBlank(message = "Dosage is required")
    @Size(max = 200, message = "Dosage must not exceed 200 characters")
    private String dosage;

    @NotBlank(message = "Frequency is required")
    @Size(max = 200, message = "Frequency must not exceed 200 characters")
    private String frequency;

    @Min(value = 1, message = "Duration in days must be at least 1")
    private int durationDays;

    @Size(max = 2000, message = "Instruction must not exceed 2000 characters")
    private String instruction;
}
