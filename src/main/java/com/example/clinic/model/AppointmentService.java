package com.example.clinic.model;

import com.example.clinic.util.ServiceStatus;
import com.example.clinic.util.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "appointment_services",
        indexes = {
                @Index(name = "idx_appointment_services_appointment", columnList = "appointment_id"),
                @Index(name = "idx_appointment_services_invoice", columnList = "invoice_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppointmentService extends BaseEntity {

    @Column(name = "appointment_id", nullable = false)
    Long appointmentId;

    @Column(name = "service_id", nullable = false)
    Long serviceId;

    @Column(name = "invoice_id")
    Long invoiceId;

    @Column(name = "service_name", nullable = false, length = 200)
    String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    ServiceType serviceType;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 0)
    BigDecimal unitPrice;

    @Column(name = "room_number", nullable = false, length = 100)
    String roomNumber;

    @Column(name = "paid_at")
    Instant paidAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    ServiceStatus status = ServiceStatus.ORDERED;

    @Column(name = "result_url", length = 2048)
    String resultUrl;

    @Column(name = "note", length = 2000)
    String note;

    @Column(name = "result_added_by")
    Long resultAddedBy;

    @Column(name = "completed_at")
    Instant completedAt;
}
