package com.example.clinic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationInputRequest {

    @NotBlank(message = "Medication name is required")
    @Size(max = 200, message = "Medication name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Strength is required")
    @Size(max = 100, message = "Strength must not exceed 100 characters")
    private String strength;

    @NotBlank(message = "Dosage form is required")
    @Size(max = 100, message = "Dosage form must not exceed 100 characters")
    private String dosageForm;

    @NotBlank(message = "Unit is required")
    @Size(max = 50, message = "Unit must not exceed 50 characters")
    private String unit;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Boolean active;
}
