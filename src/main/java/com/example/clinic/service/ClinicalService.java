package com.example.clinic.service;

import com.example.clinic.dto.request.*;
import com.example.clinic.dto.response.AppointmentResponse;
import com.example.clinic.dto.response.AppointmentServiceResponse;
import com.example.clinic.dto.response.ChargeResponse;
import com.example.clinic.exception.AppException;
import com.example.clinic.exception.ErrorCode;
import com.example.clinic.mapper.ClinicMapper;
import com.example.clinic.model.Appointment;
import com.example.clinic.model.AppointmentMedication;
import com.example.clinic.model.AppointmentService;
import com.example.clinic.model.MedicalService;
import com.example.clinic.model.Medication;
import com.example.clinic.repository.AppointmentMedicationRepository;
import com.example.clinic.repository.AppointmentServiceRepository;
import com.example.clinic.repository.MedicalServiceRepository;
import com.example.clinic.repository.MedicationRepository;
import com.example.clinic.security.Access;
import com.example.clinic.util.AppointmentStatus;
import com.example.clinic.util.ServiceStatus;
import com.example.clinic.util.ServiceType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClinicalService {

    BookingService bookingService;
    MedicalServiceRepository medicalServiceRepository;
    AppointmentServiceRepository appointmentServiceRepository;
    MedicationRepository medicationRepository;
    AppointmentMedicationRepository appointmentMedicationRepository;
    ClinicMapper clinicMapper;
    Clock clock;

    public List<AppointmentServiceResponse> order(Long appointmentId, OrderServicesRequest request) {
        Appointment appointment = bookingService.lockAppointmentWithSession(appointmentId);
        bookingService.requireAssignedDoctor(appointment);
        requireExamining(appointment);

        // 1. Kiểm tra không có dịch vụ trùng lặp ngay trong request gửi lên
        Set<Long> requestedServiceIds = new HashSet<>();
        for (OrderServiceItemRequest input : request.getItems()) {
            if (!requestedServiceIds.add(input.getServiceId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Yêu cầu có dịch vụ bị trùng lặp: serviceId=" + input.getServiceId());
            }
        }

        // 2. Kiểm tra không trùng lặp với các dịch vụ đã chỉ định trước đó chưa bị hủy
        List<AppointmentService> existingServices = appointmentServiceRepository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId);
        Set<Long> activeExistingServiceIds = new HashSet<>();
        for (AppointmentService existing : existingServices) {
            if (existing.getStatus() != ServiceStatus.CANCELLED) {
                activeExistingServiceIds.add(existing.getServiceId());
            }
        }

        List<AppointmentService> result = new ArrayList<>();
        for (OrderServiceItemRequest input : request.getItems()) {
            if (activeExistingServiceIds.contains(input.getServiceId())) {
                MedicalService service = medicalServiceRepository.findById(input.getServiceId()).orElse(null);
                String serviceName = (service != null) ? service.getName() : "ID=" + input.getServiceId();
                throw new AppException(ErrorCode.DATA_CONFLICT, "Dịch vụ đã được chỉ định trước đó cho ca khám này: " + serviceName);
            }

            MedicalService service = medicalServiceRepository.findById(input.getServiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Service not found"));

            if (!service.isActive()) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Service is inactive: " + service.getName());
            }

            AppointmentService item = AppointmentService.builder()
                    .appointmentId(appointmentId)
                    .serviceId(service.getId())
                    .serviceName(service.getName())
                    .serviceType(service.getServiceType())
                    .unitPrice(service.getPrice())
                    .roomNumber(service.getRoomNumber())
                    .note(input.getNote())
                    .status(ServiceStatus.ORDERED)
                    .build();

            result.add(appointmentServiceRepository.save(item));
        }
        return result.stream().map(clinicMapper::toAppointmentServiceResponse).toList();
    }

    public AppointmentServiceResponse cancelService(Long itemId) {
        AppointmentService item = lockedItem(itemId);
        Appointment appointment = bookingService.find(item.getAppointmentId());
        bookingService.requireAssignedDoctor(appointment);
        requireExamining(appointment);

        if (item.getStatus() != ServiceStatus.ORDERED || item.getPaidAt() != null) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Only unpaid ordered services can be cancelled");
        }

        item.setStatus(ServiceStatus.CANCELLED);
        item.setInvoiceId(null);
        return clinicMapper.toAppointmentServiceResponse(item);
    }

    public AppointmentServiceResponse startService(Long itemId) {
        AppointmentService item = lockedItem(itemId);
        Appointment appointment = bookingService.find(item.getAppointmentId());
        requireExamining(appointment);
        requirePaid(item);

        boolean isParaclinical = item.getServiceType() == ServiceType.LAB_TEST || item.getServiceType() == ServiceType.IMAGING;
        if (isParaclinical) {
            if (!Access.hasRole("ADMIN")) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Dịch vụ xét nghiệm hoặc chẩn đoán hình ảnh phải do Kỹ thuật viên / Quản trị viên tiếp nhận thực hiện");
            }
        } else {
            if (Access.hasRole("DOCTOR")) {
                bookingService.requireAssignedDoctor(appointment);
            } else if (!Access.hasRole("ADMIN")) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền tiếp nhận thực hiện dịch vụ này");
            }
        }

        if (item.getStatus() != ServiceStatus.ORDERED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Service must be ordered");
        }

        item.setStatus(ServiceStatus.IN_PROGRESS);
        return clinicMapper.toAppointmentServiceResponse(item);
    }

    public AppointmentServiceResponse completeService(Long itemId, CompleteServiceRequest request) {
        AppointmentService item = lockedItem(itemId);
        Appointment appointment = bookingService.find(item.getAppointmentId());
        requireExamining(appointment);
        requirePaid(item);

        boolean isParaclinical = item.getServiceType() == ServiceType.LAB_TEST || item.getServiceType() == ServiceType.IMAGING;
        if (isParaclinical) {
            if (!Access.hasRole("ADMIN")) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Dịch vụ xét nghiệm hoặc chẩn đoán hình ảnh phải do Kỹ thuật viên / Quản trị viên hoàn tất và tải kết quả");
            }
        } else {
            if (Access.hasRole("DOCTOR")) {
                bookingService.requireAssignedDoctor(appointment);
            } else if (!Access.hasRole("ADMIN")) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền hoàn tất dịch vụ này");
            }
        }

        if (item.getStatus() != ServiceStatus.IN_PROGRESS && item.getStatus() != ServiceStatus.COMPLETED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Service must be in progress or completed to attach result");
        }

        boolean needsFile = isParaclinical;
        String resultUrl = (request.getResultUrl() == null || request.getResultUrl().isBlank()) ? null : request.getResultUrl();
        if (needsFile && resultUrl == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lab tests and imaging require a result URL");
        }
        if (resultUrl != null) {
            validateUrl(resultUrl);
        }

        item.setResultUrl(resultUrl);
        if (request.getNote() != null) {
            item.setNote(request.getNote());
        }
        item.setResultAddedBy(Access.userId());
        item.setCompletedAt(clock.instant());
        item.setStatus(ServiceStatus.COMPLETED);

        return clinicMapper.toAppointmentServiceResponse(item);
    }

    public ChargeResponse bindServiceInvoice(Long appointmentId, BindServiceInvoiceRequest request) {
        Appointment appointment = bookingService.lockAppointmentWithSession(appointmentId);
        requireExamining(appointment);

        Set<Long> requestedIds = new HashSet<>(request.getItemIds());
        if (requestedIds.size() != request.getItemIds().size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Duplicate service item IDs");
        }

        List<AppointmentService> existing = appointmentServiceRepository.findByInvoiceIdOrderByCreatedAtAsc(request.getInvoiceId());
        if (!existing.isEmpty()) {
            Set<Long> existingIds = new HashSet<>(existing.stream().map(AppointmentService::getId).toList());
            if (!existing.get(0).getAppointmentId().equals(appointmentId) || !existingIds.equals(requestedIds)) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Invoice is already bound to a different group of services");
            }
            return charge(appointment, request.getInvoiceId(), existing);
        }

        List<AppointmentService> selected = new ArrayList<>();
        for (Long id : requestedIds) {
            AppointmentService item = appointmentServiceRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Appointment service not found"));

            if (!item.getAppointmentId().equals(appointmentId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Service does not belong to this appointment");
            }
            if (item.getStatus() != ServiceStatus.ORDERED || item.getPaidAt() != null) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Service is cancelled or already paid");
            }

            item.setInvoiceId(request.getInvoiceId());
            selected.add(item);
        }
        return charge(appointment, request.getInvoiceId(), selected);
    }

    public ChargeResponse confirmServices(PaymentConfirmedRequest request) {
        Appointment appointment = bookingService.lockAppointmentWithSession(request.getAppointmentId());
        List<AppointmentService> selected = appointmentServiceRepository.findByInvoiceIdOrderByCreatedAtAsc(request.getInvoiceId());

        if (selected.isEmpty() || selected.stream().anyMatch(i -> !i.getAppointmentId().equals(appointment.getId()))) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Invoice is not bound to this appointment's services");
        }

        ChargeResponse charge = charge(appointment, request.getInvoiceId(), selected);
        if (charge.getAmount().compareTo(request.getAmount()) != 0) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Service payment amount mismatch");
        }

        if (selected.stream().allMatch(i -> i.getPaidAt() != null)) {
            return charge;
        }

        requireExamining(appointment);

        selected.forEach(i -> {
            if (i.getStatus() != ServiceStatus.CANCELLED && i.getPaidAt() == null) {
                i.setPaidAt(clock.instant());
            }
        });
        return charge;
    }

    public AppointmentResponse completeAppointment(Long appointmentId, CompleteAppointmentRequest request) {
        Appointment appointment = bookingService.lockAppointmentWithSession(appointmentId);
        bookingService.requireAssignedDoctor(appointment);
        requireExamining(appointment);

        boolean incomplete = appointmentServiceRepository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId).stream()
                .anyMatch(i -> i.getStatus() != ServiceStatus.CANCELLED
                        && (i.getStatus() != ServiceStatus.COMPLETED || i.getPaidAt() == null));

        if (incomplete) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "All non-cancelled services must be paid and completed before finalizing");
        }

        Set<Long> seen = new HashSet<>();
        for (PrescribedMedicationRequest input : request.getMedications()) {
            if (!seen.add(input.getMedicationId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Duplicate medication in prescription");
            }

            Medication medication = medicationRepository.findById(input.getMedicationId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Medication not found"));

            if (!medication.isActive()) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Medication is inactive: " + medication.getName());
            }

            AppointmentMedication line = AppointmentMedication.builder()
                    .appointmentId(appointmentId)
                    .medicationId(medication.getId())
                    .medicineName(medication.getName())
                    .strength(medication.getStrength())
                    .unit(medication.getUnit())
                    .quantity(input.getQuantity())
                    .dosage(input.getDosage())
                    .frequency(input.getFrequency())
                    .durationDays(input.getDurationDays())
                    .instruction(input.getInstruction())
                    .build();

            appointmentMedicationRepository.save(line);
        }

        appointment.setDiagnosis(request.getDiagnosis());
        appointment.setDoctorNote(request.getDoctorNote());
        appointment.setCompletedAt(clock.instant());
        appointment.setStatus(AppointmentStatus.COMPLETED);

        return clinicMapper.toAppointmentResponse(appointment);
    }

    private AppointmentService lockedItem(Long itemId) {
        Long appointmentId = appointmentServiceRepository.findAppointmentId(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Appointment service not found"));
        bookingService.lockAppointmentWithSession(appointmentId);
        return appointmentServiceRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Appointment service not found"));
    }

    private ChargeResponse charge(Appointment appointment, Long invoiceId, List<AppointmentService> selected) {
        BigDecimal amount = selected.stream()
                .map(AppointmentService::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ChargeResponse.builder()
                .appointmentId(appointment.getId())
                .patientId(appointment.getPatientId())
                .invoiceId(invoiceId)
                .type("SERVICE")
                .amount(amount)
                .itemIds(selected.stream().map(AppointmentService::getId).toList())
                .build();
    }

    private void requireExamining(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.EXAMINING) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Appointment is not being examined");
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentServiceResponse> getServicesByAppointment(Long appointmentId) {
        return appointmentServiceRepository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId).stream()
                .map(clinicMapper::toAppointmentServiceResponse)
                .toList();
    }

    private void requirePaid(AppointmentService item) {
        if (item.getPaidAt() == null) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Service must be paid before execution or result attachment");
        }
    }

    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Result URL must be an absolute HTTP(S) URL without credentials");
        }
    }
}
