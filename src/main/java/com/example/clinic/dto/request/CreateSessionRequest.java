package com.example.clinic.dto.request;

import com.example.clinic.util.Shift;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Exam date is required")
    private LocalDate examDate;

    @NotNull(message = "Shift is required")
    private Shift shift;

    @NotNull(message = "Examination fee is required")
    @DecimalMin(value = "1", message = "Examination fee must be at least 1")
    @Digits(integer = 18, fraction = 0, message = "Examination fee must be an integer amount")
    private BigDecimal examinationFee;

    @Min(value = 1, message = "Max patients must be at least 1")
    @Max(value = 20, message = "Max patients cannot exceed 20")
    private Integer maxPatients;
}
