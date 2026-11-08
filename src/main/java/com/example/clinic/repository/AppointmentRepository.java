package com.example.clinic.repository;

import com.example.clinic.model.Appointment;
import com.example.clinic.util.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a.sessionId FROM Appointment a WHERE a.id = :id")
    Optional<Long> findSessionId(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> lockById(Long id);

    long countBySessionIdAndStatusIn(Long sessionId, Collection<AppointmentStatus> statuses);

    boolean existsBySessionIdAndPatientIdAndStatusIn(Long sessionId, Long patientId, Collection<AppointmentStatus> statuses);

    Page<Appointment> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    List<Appointment> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Appointment> findBySessionIdOrderByQueueNumberAscCreatedAtAsc(Long sessionId);

    List<Appointment> findBySessionIdAndStatusInOrderByQueueNumberAscCreatedAtAsc(Long sessionId, Collection<AppointmentStatus> statuses);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Appointment a SET a.status = :expired, a.version = a.version + 1 WHERE a.sessionId = :sessionId AND a.status = :pending AND a.expiresAt <= :now")
    int expirePending(Long sessionId, Instant now, AppointmentStatus pending, AppointmentStatus expired);

    @Query("SELECT DISTINCT a.sessionId FROM Appointment a WHERE a.status = :status AND a.expiresAt <= :now")
    List<Long> findExpiredSessionIds(AppointmentStatus status, Instant now);

    @Query("SELECT a FROM Appointment a WHERE a.sessionId = :sessionId AND a.status = :status AND a.expiresAt <= :now")
    List<Appointment> findExpiredPendingAppointments(Long sessionId, AppointmentStatus status, Instant now);

    List<Appointment> findByCreatedAtBetween(Instant start, Instant end);

    List<Appointment> findBySessionIdIn(Collection<Long> sessionIds);

    @Query("SELECT a FROM Appointment a WHERE a.examinationInvoiceId IS NULL " +
            "AND a.examinationFee > 0 " +
            "AND a.status IN :statuses " +
            "ORDER BY a.id ASC")
    List<Appointment> findMissingExaminationInvoices(@Param("statuses") Collection<AppointmentStatus> statuses);
}
