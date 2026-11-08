package com.example.clinic.dto.request;

import com.example.clinic.util.ServiceType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInputRequest {

    @NotBlank(message = "Service name is required")
    @Size(max = 200, message = "Service name must not exceed 200 characters")
    private String name;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1", message = "Price must be at least 1")
    @Digits(integer = 18, fraction = 0, message = "Price must be an integer amount")
    private BigDecimal price;

    @NotBlank(message = "Room number is required")
    @Size(max = 100, message = "Room number must not exceed 100 characters")
    private String roomNumber;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Boolean active;
}
