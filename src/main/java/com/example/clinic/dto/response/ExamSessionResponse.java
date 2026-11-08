package com.example.clinic.dto.response;

import com.example.clinic.util.SessionStatus;
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
public class ExamSessionResponse {

    private Long id;
    private Instant createdAt;
    private long version;
    private Long doctorId;
    private String doctorName;
    private String doctorRank;
    private String doctorAvatar;
    private String doctorPhone;
    private LocalDate examDate;
    private Shift shift;
    private BigDecimal examinationFee;
    private int maxPatients;
    private int lastQueueNumber;
    private SessionStatus status;
    private Integer availableSlots;
}


