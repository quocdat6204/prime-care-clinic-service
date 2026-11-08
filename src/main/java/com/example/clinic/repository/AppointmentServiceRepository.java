package com.example.clinic.repository;

import com.example.clinic.model.AppointmentService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentServiceRepository extends JpaRepository<AppointmentService, Long> {

    @Query("SELECT s.appointmentId FROM AppointmentService s WHERE s.id = :id")
    Optional<Long> findAppointmentId(Long id);

    List<AppointmentService> findByAppointmentIdOrderByCreatedAtAsc(Long appointmentId);

    List<AppointmentService> findByInvoiceIdOrderByCreatedAtAsc(Long invoiceId);

    List<AppointmentService> findByCreatedAtBetween(java.time.Instant start, java.time.Instant end);
}
