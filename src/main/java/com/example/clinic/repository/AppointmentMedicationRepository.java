package com.example.clinic.repository;

import com.example.clinic.model.AppointmentMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentMedicationRepository extends JpaRepository<AppointmentMedication, Long> {

    List<AppointmentMedication> findByAppointmentIdOrderByCreatedAtAsc(Long appointmentId);

    void deleteByAppointmentId(Long appointmentId);

    List<AppointmentMedication> findByCreatedAtBetween(java.time.Instant start, java.time.Instant end);
}
