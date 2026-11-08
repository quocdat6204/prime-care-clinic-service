package com.example.clinic.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderServiceItemRequest {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;
}
