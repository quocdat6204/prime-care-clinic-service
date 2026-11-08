package com.example.clinic.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAppointmentRequest {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @Size(max = 2000, message = "Symptoms description must not exceed 2000 characters")
    private String symptoms;
}
