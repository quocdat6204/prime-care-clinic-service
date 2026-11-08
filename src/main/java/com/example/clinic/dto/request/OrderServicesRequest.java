package com.example.clinic.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderServicesRequest {

    @NotEmpty(message = "Service order list cannot be empty")
    @Size(max = 50, message = "Cannot order more than 50 services at once")
    private List<@NotNull @Valid OrderServiceItemRequest> items;
}
