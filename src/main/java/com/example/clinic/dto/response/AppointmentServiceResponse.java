package com.example.clinic.dto.response;

import com.example.clinic.util.ServiceStatus;
import com.example.clinic.util.ServiceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentServiceResponse {

    private Long id;
    private Instant createdAt;
    private long version;
    private Long appointmentId;
    private Long serviceId;
    private Long invoiceId;
    private String serviceName;
    private ServiceType serviceType;
    private BigDecimal unitPrice;
    private String roomNumber;
    private Instant paidAt;
    private ServiceStatus status;
    private String resultUrl;
    private String note;
    private Long resultAddedBy;
    private Instant completedAt;
}

