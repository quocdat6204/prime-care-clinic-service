package com.example.clinic.repository;

import com.example.clinic.model.ExamSession;
import com.example.clinic.util.SessionStatus;
import com.example.clinic.util.Shift;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long>, JpaSpecificationExecutor<ExamSession> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ExamSession s WHERE s.id = :id")
    Optional<ExamSession> lockById(Long id);

    List<ExamSession> findByExamDateOrderByShiftAscDoctorIdAsc(LocalDate date);

    boolean existsByExamDateAndShiftAndDoctorId(LocalDate date, Shift shift, Long doctorId);

    Page<ExamSession> findByExamDateAndStatusOrderByShiftAscDoctorIdAsc(LocalDate date, SessionStatus status, Pageable pageable);

    Page<ExamSession> findByExamDateAndDoctorIdAndStatusOrderByShiftAsc(LocalDate date, Long doctorId, SessionStatus status, Pageable pageable);

    Page<ExamSession> findByDoctorIdAndExamDateOrderByShiftAsc(Long doctorId, LocalDate date, Pageable pageable);

    Page<ExamSession> findByDoctorIdOrderByExamDateDescShiftAsc(Long doctorId, Pageable pageable);

    List<ExamSession> findByExamDateBetween(LocalDate from, LocalDate to);
}

