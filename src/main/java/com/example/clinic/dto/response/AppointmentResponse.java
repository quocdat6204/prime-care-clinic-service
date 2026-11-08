package com.example.clinic.dto.response;

import com.example.clinic.util.AppointmentStatus;
import com.example.clinic.util.Shift;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;
    private Instant createdAt;
    private long version;
    private Long sessionId;
    private Long patientId;
    private Integer queueNumber;
    private BigDecimal examinationFee;
    private AppointmentStatus status;
    private Instant expiresAt;
    private Long examinationInvoiceId;
    private Instant examinationPaidAt;
    private String symptoms;
    private String diagnosis;
    private String doctorNote;
    private Instant completedAt;

    // Thông tin bệnh nhân enriched từ user-service
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private String patientGender;
    private String patientAddress;
    private String patientAvatar;

    // Thông tin ca khám và bác sĩ phụ trách
    private LocalDate examDate;
    private Shift shift;
    private Long doctorId;
    private String doctorName;
    private String doctorRank;
}
