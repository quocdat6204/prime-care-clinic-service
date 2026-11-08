package com.example.clinic.dto.response;

import com.example.clinic.util.ServiceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalServiceResponse {

    private Long id;
    private Instant createdAt;
    private long version;
    private String name;
    private ServiceType serviceType;
    private BigDecimal price;
    private String roomNumber;
    private String description;
    private boolean active;
}
