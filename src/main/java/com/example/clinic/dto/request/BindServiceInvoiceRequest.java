package com.example.clinic.dto.request;

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
public class BindServiceInvoiceRequest {

    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    @NotEmpty(message = "Service item IDs list cannot be empty")
    @Size(max = 50, message = "Cannot bind more than 50 service items at once")
    private List<@NotNull Long> itemIds;
}
