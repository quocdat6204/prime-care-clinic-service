package com.example.clinic.controller;

import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.ExamSessionResponse;
import com.example.clinic.dto.response.MedicalServiceResponse;
import com.example.clinic.mapper.ClinicMapper;
import com.example.clinic.model.MedicalService;
import com.example.clinic.repository.MedicalServiceRepository;
import com.example.clinic.service.BookingService;
import com.example.clinic.util.ServiceType;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller nội bộ phục vụ chatbot-service gọi qua OpenFeign.
 */
@RestController
@RequestMapping("/internal/clinic/chatbot")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalChatbotController {

    BookingService bookingService;
    MedicalServiceRepository medicalServiceRepository;
    ClinicMapper clinicMapper;

    /**
     * Tra cứu ca khám đang mở theo ngày và bác sĩ (tùy chọn) cho Trợ lý AI.
     */
    @GetMapping("/sessions")
    public ApiResponse<List<ExamSessionResponse>> getOpenSessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long doctorId) {

        Page<ExamSessionResponse> page = bookingService.patientSessions(
                date,
                doctorId,
                null,
                PageRequest.of(0, 50)
        );

        return ApiResponse.<List<ExamSessionResponse>>builder()
                .result(page.getContent())
                .build();
    }

    /**
     * Tra cứu dịch vụ y tế theo từ khóa cho Trợ lý AI.
     */
    @GetMapping("/services")
    public ApiResponse<List<MedicalServiceResponse>> getActiveServices(
            @RequestParam(required = false) String keyword) {

        Specification<MedicalService> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                List<Predicate> orPredicates = new ArrayList<>();
                orPredicates.add(cb.like(cb.lower(root.get("name")), pattern));
                orPredicates.add(cb.like(cb.lower(root.get("description")), pattern));
                orPredicates.add(cb.like(cb.lower(root.get("roomNumber")), pattern));

                String lower = keyword.trim().toLowerCase();
                if (lower.contains("xét nghiệm") || lower.contains("xet nghiem") || lower.contains("máu") || lower.contains("mau") || lower.contains("cbc") || lower.contains("lab")) {
                    orPredicates.add(cb.equal(root.get("serviceType"), ServiceType.LAB_TEST));
                }
                if (lower.contains("x-quang") || lower.contains("xquang") || lower.contains("chụp") || lower.contains("chup") || lower.contains("siêu âm") || lower.contains("sieu am") || lower.contains("hình ảnh")) {
                    orPredicates.add(cb.equal(root.get("serviceType"), ServiceType.IMAGING));
                }
                if (lower.contains("răng") || lower.contains("rang") || lower.contains("nha")) {
                    orPredicates.add(cb.equal(root.get("serviceType"), ServiceType.DENTAL));
                }

                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<MedicalServiceResponse> list = medicalServiceRepository
                .findAll(spec, PageRequest.of(0, 15, Sort.by("name").ascending()))
                .map(clinicMapper::toMedicalServiceResponse)
                .getContent();

        return ApiResponse.<List<MedicalServiceResponse>>builder()
                .result(list)
                .build();
    }
}

