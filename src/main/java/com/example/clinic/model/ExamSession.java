package com.example.clinic.model;

import com.example.clinic.util.SessionStatus;
import com.example.clinic.util.Shift;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "exam_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_session_doctor_date_shift", columnNames = {"exam_date", "shift", "doctor_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamSession extends BaseEntity {

    @Column(name = "doctor_id", nullable = false)
    Long doctorId;

    @Column(name = "exam_date", nullable = false)
    LocalDate examDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", nullable = false, length = 30)
    Shift shift;

    @Column(name = "examination_fee", nullable = false, precision = 18, scale = 0)
    BigDecimal examinationFee;

    @Builder.Default
    @Column(name = "max_patients", nullable = false)
    int maxPatients = 20;

    @Builder.Default
    @Column(name = "last_queue_number", nullable = false)
    int lastQueueNumber = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    SessionStatus status = SessionStatus.OPEN;
}
