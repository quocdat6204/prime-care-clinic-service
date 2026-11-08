package com.example.clinic.repository;

import com.example.clinic.model.Medication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long>, JpaSpecificationExecutor<Medication> {

    Page<Medication> findByActiveTrue(Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCaseAndStrengthIgnoreCase(String name, String strength);

    boolean existsByNameIgnoreCaseAndStrengthIgnoreCaseAndIdNot(String name, String strength, Long id);
}
