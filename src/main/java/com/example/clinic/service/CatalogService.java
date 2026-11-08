package com.example.clinic.service;

import com.example.clinic.dto.request.MedicationInputRequest;
import com.example.clinic.dto.request.ServiceInputRequest;
import com.example.clinic.dto.response.MedicalServiceResponse;
import com.example.clinic.dto.response.MedicationResponse;
import com.example.clinic.exception.AppException;
import com.example.clinic.exception.ErrorCode;
import com.example.clinic.mapper.ClinicMapper;
import com.example.clinic.model.MedicalService;
import com.example.clinic.model.Medication;
import com.example.clinic.repository.MedicalServiceRepository;
import com.example.clinic.repository.MedicationRepository;
import com.example.clinic.security.Access;
import com.example.clinic.util.ServiceType;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogService {

    MedicalServiceRepository medicalServiceRepository;
    MedicationRepository medicationRepository;
    ClinicMapper clinicMapper;

    @Transactional(readOnly = true)
    public Page<MedicalServiceResponse> services(ServiceType serviceType, String keyword, boolean includeInactive, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
        }
        boolean isAdmin = Access.hasRole("ADMIN");
        Specification<MedicalService> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Doctor chỉ được xem các dịch vụ đang active; Admin chỉ xem inactive khi includeInactive = true
            if (!isAdmin || !includeInactive) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            if (serviceType != null) {
                predicates.add(cb.equal(root.get("serviceType"), serviceType));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return medicalServiceRepository.findAll(spec, pageable).map(clinicMapper::toMedicalServiceResponse);
    }

    @Transactional(readOnly = true)
    public Page<MedicationResponse> medications(String keyword, boolean includeInactive, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
        }
        boolean isAdmin = Access.hasRole("ADMIN");
        Specification<Medication> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Doctor chỉ được xem các thuốc đang active; Admin chỉ xem inactive khi includeInactive = true
            if (!isAdmin || !includeInactive) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return medicationRepository.findAll(spec, pageable).map(clinicMapper::toMedicationResponse);
    }

    public MedicalServiceResponse saveService(Long id, ServiceInputRequest request) {
        String trimmedName = request.getName().trim();
        if (id == null) {
            if (medicalServiceRepository.existsByNameIgnoreCase(trimmedName)) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Service name already exists");
            }
        } else {
            if (medicalServiceRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Service name already exists");
            }
        }

        MedicalService service = (id == null)
                ? new MedicalService()
                : medicalServiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Service not found"));

        service.setName(trimmedName);
        service.setServiceType(request.getServiceType());
        service.setPrice(request.getPrice());
        service.setRoomNumber(request.getRoomNumber() != null ? request.getRoomNumber().trim() : null);
        service.setDescription(request.getDescription());
        service.setActive(request.getActive() == null || request.getActive());

        return clinicMapper.toMedicalServiceResponse(medicalServiceRepository.save(service));
    }

    public MedicationResponse saveMedication(Long id, MedicationInputRequest request) {
        String trimmedName = request.getName().trim();
        String trimmedStrength = request.getStrength().trim();
        if (id == null) {
            if (medicationRepository.existsByNameIgnoreCaseAndStrengthIgnoreCase(trimmedName, trimmedStrength)) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Medication with the same name and strength already exists");
            }
        } else {
            if (medicationRepository.existsByNameIgnoreCaseAndStrengthIgnoreCaseAndIdNot(trimmedName, trimmedStrength, id)) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Medication with the same name and strength already exists");
            }
        }

        Medication medication = (id == null)
                ? new Medication()
                : medicationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Medication not found"));

        medication.setName(trimmedName);
        medication.setStrength(trimmedStrength);
        medication.setDosageForm(request.getDosageForm() != null ? request.getDosageForm().trim() : null);
        medication.setUnit(request.getUnit() != null ? request.getUnit().trim() : null);
        medication.setDescription(request.getDescription());
        medication.setActive(request.getActive() == null || request.getActive());

        return clinicMapper.toMedicationResponse(medicationRepository.save(medication));
    }

    public void deleteService(Long id) {
        MedicalService service = medicalServiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Service not found"));
        medicalServiceRepository.delete(service);
    }

    public void deleteMedication(Long id) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Medication not found"));
        medicationRepository.delete(medication);
    }

    public MedicalServiceResponse toggleServiceStatus(Long id) {
        MedicalService service = medicalServiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Service not found"));
        service.setActive(!service.isActive());
        return clinicMapper.toMedicalServiceResponse(medicalServiceRepository.save(service));
    }

    public MedicationResponse toggleMedicationStatus(Long id) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Medication not found"));
        medication.setActive(!medication.isActive());
        return clinicMapper.toMedicationResponse(medicationRepository.save(medication));
    }
}
