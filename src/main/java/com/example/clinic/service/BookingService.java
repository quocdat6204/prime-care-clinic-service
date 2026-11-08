package com.example.clinic.service;

import com.example.clinic.client.PaymentClient;
import com.example.clinic.client.UserClient;
import com.example.clinic.dto.request.BookAppointmentRequest;
import com.example.clinic.dto.request.CreateSessionRequest;
import com.example.clinic.dto.request.PaymentConfirmedRequest;
import com.example.clinic.dto.request.WalkInAppointmentRequest;
import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.AppointmentDetailResponse;
import com.example.clinic.dto.response.AppointmentPaymentCheckResponse;
import com.example.clinic.dto.response.AppointmentResponse;
import com.example.clinic.dto.response.ChargeResponse;
import com.example.clinic.dto.response.DoctorSummaryResponse;
import com.example.clinic.dto.response.ExamSessionResponse;
import com.example.clinic.dto.response.UserSummaryResponse;
import com.example.clinic.exception.AppException;
import com.example.clinic.exception.ErrorCode;
import com.example.clinic.mapper.ClinicMapper;
import com.example.clinic.model.Appointment;
import com.example.clinic.model.ExamSession;
import com.example.clinic.repository.AppointmentMedicationRepository;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.AppointmentServiceRepository;
import com.example.clinic.repository.ExamSessionRepository;
import com.example.clinic.security.Access;
import com.example.clinic.util.AppointmentStatus;
import com.example.clinic.util.ServiceStatus;
import com.example.clinic.util.SessionStatus;
import com.example.clinic.util.Shift;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingService {

    final ExamSessionRepository examSessionRepository;
    final AppointmentRepository appointmentRepository;
    final AppointmentServiceRepository appointmentServiceRepository;
    final AppointmentMedicationRepository appointmentMedicationRepository;
    final ClinicMapper clinicMapper;
    final Clock clock;
    final UserClient userClient;
    final PaymentClient paymentClient;

    @Value("${clinic.booking-hold-minutes:10}")
    int holdMinutes;

    @Value("${clinic.timezone:Asia/Ho_Chi_Minh}")
    String timezone;

    static final List<AppointmentStatus> OCCUPIED = List.of(
            AppointmentStatus.PENDING_PAYMENT,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.EXAMINING,
            AppointmentStatus.COMPLETED
    );

    static final List<AppointmentStatus> DOCTOR_VIEWABLE_STATUSES = List.of(
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.EXAMINING,
            AppointmentStatus.COMPLETED
    );

    public ExamSessionResponse createSession(CreateSessionRequest request) {
        if (request.getExamDate().isBefore(today())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Exam date cannot be in the past");
        }

        Long doctorId = request.getDoctorId();
        if (examSessionRepository.existsByExamDateAndShiftAndDoctorId(request.getExamDate(), request.getShift(), doctorId)) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "This doctor already has a session on this date and shift");
        }

        ExamSession session = ExamSession.builder()
                .doctorId(doctorId)
                .examDate(request.getExamDate())
                .shift(request.getShift())
                .examinationFee(request.getExaminationFee())
                .maxPatients(request.getMaxPatients() == null ? 20 : request.getMaxPatients())
                .lastQueueNumber(0)
                .status(SessionStatus.OPEN)
                .build();

        return clinicMapper.toExamSessionResponse(examSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public Page<ExamSessionResponse> patientSessions(LocalDate date, Long doctorId, Shift shift, Pageable pageable) {
        if (date.isBefore(today())) {
            return Page.empty(pageable);
        }
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by("shift").ascending().and(Sort.by("doctorId").ascending()));
        }
        Specification<ExamSession> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("examDate"), date));
            predicates.add(cb.equal(root.get("status"), SessionStatus.OPEN));
            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctorId"), doctorId));
            }
            if (shift != null) {
                predicates.add(cb.equal(root.get("shift"), shift));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ExamSessionResponse> responses = examSessionRepository.findAll(spec, pageable).map(this::toResponseWithSlots);
        enrichDoctors(responses.getContent());
        return responses;
    }

    @Transactional(readOnly = true)
    public Page<ExamSessionResponse> doctorSessions(LocalDate date, Pageable pageable) {
        Long doctorId = Access.userId();
        Page<ExamSession> page;
        if (date != null) {
            page = examSessionRepository.findByDoctorIdAndExamDateOrderByShiftAsc(doctorId, date, pageable);
        } else {
            page = examSessionRepository.findByDoctorIdOrderByExamDateDescShiftAsc(doctorId, pageable);
        }
        Page<ExamSessionResponse> responses = page.map(this::toResponseWithSlots);
        enrichDoctors(responses.getContent());
        return responses;
    }

    @Transactional(readOnly = true)
    public Page<ExamSessionResponse> adminSessions(LocalDate date, Long doctorId, Shift shift, SessionStatus status, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "id"));
        }
        Specification<ExamSession> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (date != null) {
                predicates.add(cb.equal(root.get("examDate"), date));
            }
            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctorId"), doctorId));
            }
            if (shift != null) {
                predicates.add(cb.equal(root.get("shift"), shift));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ExamSessionResponse> responses = examSessionRepository.findAll(spec, pageable).map(this::toResponseWithSlots);
        enrichDoctors(responses.getContent());
        return responses;
    }


    private ExamSessionResponse toResponseWithSlots(ExamSession session) {
        long occupied = appointmentRepository.countBySessionIdAndStatusIn(session.getId(), OCCUPIED);
        int availableSlots = Math.max(0, session.getMaxPatients() - (int) occupied);
        return clinicMapper.toExamSessionResponse(session, availableSlots);
    }

    private void enrichDoctors(List<ExamSessionResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        Set<Long> doctorIds = responses.stream()
                .map(ExamSessionResponse::getDoctorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (doctorIds.isEmpty()) {
            return;
        }
        try {
            ApiResponse<List<DoctorSummaryResponse>> apiResponse = userClient.getDoctorsBatch(new ArrayList<>(doctorIds));
            if (apiResponse != null && apiResponse.getResult() != null) {
                Map<Long, DoctorSummaryResponse> doctorMap = apiResponse.getResult().stream()
                        .filter(d -> d.getId() != null)
                        .collect(Collectors.toMap(DoctorSummaryResponse::getId, d -> d, (e1, e2) -> e1));

                for (ExamSessionResponse s : responses) {
                    if (s.getDoctorId() != null) {
                        DoctorSummaryResponse doc = doctorMap.get(s.getDoctorId());
                        if (doc != null) {
                            s.setDoctorName(doc.getFullName());
                            s.setDoctorRank(doc.getRank());
                            s.setDoctorAvatar(doc.getAva());
                            s.setDoctorPhone(doc.getPhone());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Cannot fetch doctor information from user-service: {}", e.getMessage());
        }
    }

    private void enrichPatients(List<AppointmentResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        Set<Long> patientIds = responses.stream()
                .map(AppointmentResponse::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (patientIds.isEmpty()) {
            return;
        }
        try {
            ApiResponse<List<UserSummaryResponse>> apiResponse = userClient.getUsersBatch(new ArrayList<>(patientIds));
            if (apiResponse != null && apiResponse.getResult() != null) {
                Map<Long, UserSummaryResponse> userMap = apiResponse.getResult().stream()
                        .filter(u -> u.getId() != null)
                        .collect(Collectors.toMap(UserSummaryResponse::getId, u -> u, (e1, e2) -> e1));

                for (AppointmentResponse apt : responses) {
                    if (apt.getPatientId() != null) {
                        UserSummaryResponse user = userMap.get(apt.getPatientId());
                        if (user != null) {
                            apt.setPatientName(user.getFullName());
                            apt.setPatientPhone(user.getPhone());
                            apt.setPatientEmail(user.getEmail());
                            apt.setPatientAddress(user.getAddress());
                            apt.setPatientGender(user.getGender());
                            apt.setPatientAvatar(user.getAva());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Cannot fetch patient information from user-service: {}", e.getMessage());
        }
    }

    private void enrichSessions(List<AppointmentResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        Set<Long> sessionIds = responses.stream()
                .map(AppointmentResponse::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return;
        }
        List<ExamSession> sessions = examSessionRepository.findAllById(sessionIds);
        Map<Long, ExamSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(ExamSession::getId, s -> s, (s1, s2) -> s1));

        Set<Long> doctorIds = sessions.stream()
                .map(ExamSession::getDoctorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, DoctorSummaryResponse> doctorMap = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            try {
                ApiResponse<List<DoctorSummaryResponse>> apiResponse = userClient.getDoctorsBatch(new ArrayList<>(doctorIds));
                if (apiResponse != null && apiResponse.getResult() != null) {
                    doctorMap = apiResponse.getResult().stream()
                            .filter(d -> d.getId() != null)
                            .collect(Collectors.toMap(DoctorSummaryResponse::getId, d -> d, (e1, e2) -> e1));
                }
            } catch (Exception e) {
                log.warn("Cannot fetch doctor information from user-service: {}", e.getMessage());
            }
        }

        for (AppointmentResponse apt : responses) {
            if (apt.getSessionId() != null) {
                ExamSession s = sessionMap.get(apt.getSessionId());
                if (s != null) {
                    apt.setExamDate(s.getExamDate());
                    apt.setShift(s.getShift());
                    apt.setDoctorId(s.getDoctorId());
                    if (s.getDoctorId() != null) {
                        DoctorSummaryResponse doc = doctorMap.get(s.getDoctorId());
                        if (doc != null) {
                            apt.setDoctorName(doc.getFullName());
                            apt.setDoctorRank(doc.getRank());
                        }
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        Access.require(Access.hasRole("ADMIN"));
        List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        List<AppointmentResponse> responses = appointments.stream()
                .map(clinicMapper::toAppointmentResponse)
                .toList();
        enrichPatients(responses);
        enrichSessions(responses);
        return responses;
    }



    public ExamSessionResponse toggleSessionStatus(Long id) {
        ExamSession session = lockSession(id);
        SessionStatus newStatus = (session.getStatus() == SessionStatus.OPEN)
                ? SessionStatus.CLOSED
                : SessionStatus.OPEN;
        session.setStatus(newStatus);
        ExamSessionResponse response = toResponseWithSlots(session);
        enrichDoctors(List.of(response));
        return response;
    }


    public AppointmentResponse book(BookAppointmentRequest request) {
        ExamSession session = lockSession(request.getSessionId()); // Khóa mọi tracsaction khi đang đọc dữ liệu để tránh đặt quá số lượng
        expire(session.getId()); // Chuyển mọi giao dịch đã quá hạn thanh toán thành EXPIRED

        if (session.getStatus() != SessionStatus.OPEN || session.getExamDate().isBefore(today())) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Session is closed or in the past");
        }

        if (appointmentRepository.existsBySessionIdAndPatientIdAndStatusIn(session.getId(), Access.userId(), OCCUPIED)) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "You already have an active appointment in this session");
        }

        if (appointmentRepository.countBySessionIdAndStatusIn(session.getId(), OCCUPIED) >= session.getMaxPatients()) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Session is full");
        }

        Appointment appointment = Appointment.builder()
                .sessionId(session.getId())
                .patientId(Access.userId())
                .examinationFee(session.getExaminationFee())
                .symptoms(request.getSymptoms())
                .status(AppointmentStatus.PENDING_PAYMENT)
                .expiresAt(clock.instant().plus(Duration.ofMinutes(holdMinutes)))
                .build();

        return clinicMapper.toAppointmentResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse cancel(Long id) {
        Appointment appointment = lockAppointmentWithSession(id);
        Access.require(appointment.getPatientId().equals(Access.userId()));

        if (appointment.getStatus() != AppointmentStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Only unpaid pending appointments can be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return clinicMapper.toAppointmentResponse(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentDetailResponse detail(Long id) {
        Appointment appointment = find(id);
        ExamSession session = examSessionRepository.findById(appointment.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found"));

        canRead(appointment, session);

        ExamSessionResponse sessionResponse = toResponseWithSlots(session);
        enrichDoctors(List.of(sessionResponse));

        AppointmentResponse appointmentResponse = clinicMapper.toAppointmentResponse(appointment);
        enrichPatients(List.of(appointmentResponse));

        var servicesStream = appointmentServiceRepository.findByAppointmentIdOrderByCreatedAtAsc(id).stream();
        if (!Access.hasRole("ADMIN") && !Access.hasRole("DOCTOR")) {
            servicesStream = servicesStream.filter(s -> s.getStatus() != ServiceStatus.CANCELLED);
        }

        return AppointmentDetailResponse.builder()
                .appointment(appointmentResponse)
                .session(sessionResponse)
                .services(servicesStream.map(clinicMapper::toAppointmentServiceResponse).toList())
                .medications(appointmentMedicationRepository.findByAppointmentIdOrderByCreatedAtAsc(id).stream()
                        .map(clinicMapper::toAppointmentMedicationResponse).toList())
                .build();
    }


    @Transactional(readOnly = true)
    public Page<AppointmentResponse> mine(Pageable pageable) {
        return appointmentRepository.findByPatientIdOrderByCreatedAtDesc(Access.userId(), pageable)
                .map(clinicMapper::toAppointmentResponse);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> sessionAppointments(Long id) {
        return sessionAppointments(id, null);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> sessionAppointments(Long id, AppointmentStatus status) {
        ExamSession session = examSessionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found"));

        Access.require(Access.hasRole("ADMIN")
                || (Access.hasRole("DOCTOR") && session.getDoctorId().equals(Access.userId())));

        List<Appointment> appointments;
        if (!Access.hasRole("ADMIN")) {
            // Đối với Bác sĩ: chỉ xem các lịch hẹn hợp lệ đã xác nhận hoặc đang/đã khám (CONFIRMED, EXAMINING, COMPLETED)
            if (status != null) {
                if (!DOCTOR_VIEWABLE_STATUSES.contains(status)) {
                    return List.of();
                }
                appointments = appointmentRepository.findBySessionIdAndStatusInOrderByQueueNumberAscCreatedAtAsc(
                        id, List.of(status));
            } else {
                appointments = appointmentRepository.findBySessionIdAndStatusInOrderByQueueNumberAscCreatedAtAsc(
                        id, DOCTOR_VIEWABLE_STATUSES);
            }
        } else {
            // Đối với ADMIN: xem theo status nếu có, hoặc tất cả mọi trạng thái
            if (status != null) {
                appointments = appointmentRepository.findBySessionIdAndStatusInOrderByQueueNumberAscCreatedAtAsc(
                        id, List.of(status));
            } else {
                appointments = appointmentRepository.findBySessionIdOrderByQueueNumberAscCreatedAtAsc(id);
            }
        }

        List<AppointmentResponse> responseList = appointments.stream()
                .map(clinicMapper::toAppointmentResponse)
                .toList();
        enrichPatients(responseList);
        return responseList;
    }

    public AppointmentResponse start(Long id) {
        Appointment appointment = lockAppointmentWithSession(id);
        requireAssignedDoctor(appointment);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Appointment must be paid and confirmed");
        }

        appointment.setStatus(AppointmentStatus.EXAMINING);
        return clinicMapper.toAppointmentResponse(appointment);
    }

    public AppointmentResponse confirmExamination(PaymentConfirmedRequest request) {
        Appointment appointment = lockAppointmentWithSession(request.getAppointmentId());

        if (appointment.getExaminationFee().compareTo(request.getAmount()) != 0) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Examination amount mismatch");
        }

        if (appointment.getExaminationPaidAt() != null) {
            if (appointment.getExaminationInvoiceId() != null
                    && !request.getInvoiceId().equals(appointment.getExaminationInvoiceId())) {
                throw new AppException(ErrorCode.DATA_CONFLICT, "Already paid using a different invoice");
            }
            if (appointment.getExaminationInvoiceId() == null) {
                appointment.setExaminationInvoiceId(request.getInvoiceId());
            }
            return clinicMapper.toAppointmentResponse(appointment);
        }

        Instant paidAt = request.getPaidAt() != null ? request.getPaidAt() : clock.instant();
        // Dung sai 60 giây bù đắp lệch đồng hồ giữa các server cloud và độ trễ mạng callback
        boolean paidOnTime = appointment.getExpiresAt() != null
                && paidAt.isBefore(appointment.getExpiresAt().plus(Duration.ofSeconds(60)));

        ExamSession session = lockSession(appointment.getSessionId());

        // Kiểm tra xem ca khám có còn chỗ khả dụng không
        long currentOccupied = appointmentRepository.countBySessionIdAndStatusIn(session.getId(), OCCUPIED);
        boolean isAlreadyOccupied = (appointment.getStatus() == AppointmentStatus.PENDING_PAYMENT);
        boolean hasSlotAvailable = isAlreadyOccupied || (currentOccupied < session.getMaxPatients());

        // Khách trả tiền đúng hạn HOẶC ca khám vẫn còn slot khả dụng -> Xác nhận hợp lệ
        if (paidOnTime || hasSlotAvailable) {
            session.setLastQueueNumber(session.getLastQueueNumber() + 1);

            appointment.setQueueNumber(session.getLastQueueNumber());
            appointment.setExaminationInvoiceId(request.getInvoiceId());
            appointment.setExaminationPaidAt(paidAt);
            appointment.setStatus(AppointmentStatus.CONFIRMED);

            return clinicMapper.toAppointmentResponse(appointment);
        } else {
            // Khách trả trễ và ca khám đã kín chỗ -> Đánh dấu chờ hoàn tiền
            appointment.setExaminationInvoiceId(request.getInvoiceId());
            appointment.setExaminationPaidAt(paidAt);
            appointment.setStatus(AppointmentStatus.REFUND_PENDING);

            return clinicMapper.toAppointmentResponse(appointment);
        }
    }

    /**
     * Tiếp nhận bệnh nhân khám trực tiếp tại quầy (Dành cho ADMIN).
     * Cho phép thêm bệnh nhân ngay cả khi ca khám đã hết slot (Overbooking).
     * Bệnh nhân thanh toán trực tiếp tại quầy -> tự động tạo Invoice EXAMINATION PAID
     * và chuyển trực tiếp sang trạng thái CONFIRMED kèm số thứ tự (queueNumber).
     */
    public AppointmentResponse walkIn(Long sessionId, WalkInAppointmentRequest request) {
        Access.require(Access.hasRole("ADMIN"));

        ExamSession session = lockSession(sessionId);
        expire(session.getId());

        if (session.getStatus() != SessionStatus.OPEN || session.getExamDate().isBefore(today())) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Ca khám đã đóng hoặc đã ở trong quá khứ");
        }

        if (appointmentRepository.existsBySessionIdAndPatientIdAndStatusIn(session.getId(), request.getPatientId(), OCCUPIED)) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Bệnh nhân này đã có lịch hẹn đang hoạt động trong ca khám này");
        }

        // Cấp số thứ tự tiếp theo cho bệnh nhân (Overbooking: không chặn khi hết slot)
        int nextQueue = session.getLastQueueNumber() + 1;
        session.setLastQueueNumber(nextQueue);
        examSessionRepository.save(session);

        Instant now = clock.instant();

        Appointment appointment = Appointment.builder()
                .sessionId(session.getId())
                .patientId(request.getPatientId())
                .queueNumber(nextQueue)
                .examinationFee(session.getExaminationFee() != null ? session.getExaminationFee() : BigDecimal.ZERO)
                .symptoms(request.getSymptoms())
                .status(AppointmentStatus.CONFIRMED)
                .expiresAt(now.plus(Duration.ofDays(1)))
                .examinationPaidAt(now)
                .build();

        appointment = appointmentRepository.save(appointment);

        // Tự động thu tiền mặt phí khám tại quầy qua payment-service
        if (appointment.getExaminationFee().compareTo(BigDecimal.ZERO) > 0) {
            try {
                ApiResponse<Long> invoiceRes = paymentClient.recordCashExamination(
                        appointment.getId(),
                        appointment.getPatientId(),
                        appointment.getExaminationFee()
                );
                if (invoiceRes != null && invoiceRes.getResult() != null) {
                    appointment.setExaminationInvoiceId(invoiceRes.getResult());
                    appointment = appointmentRepository.save(appointment);
                }
            } catch (Exception e) {
                log.error("Lỗi khi ghi nhận hóa đơn tiền mặt cho tiếp nhận tại quầy appointmentId={}: {}",
                        appointment.getId(), e.getMessage(), e);
            }
        }

        AppointmentResponse response = clinicMapper.toAppointmentResponse(appointment);
        enrichPatients(List.of(response));
        return response;
    }

    public ChargeResponse examinationCharge(Long id) {
        Appointment appointment = lockAppointmentWithSession(id);
        if (appointment.getStatus() != AppointmentStatus.PENDING_PAYMENT || !appointment.getExpiresAt().isAfter(clock.instant())) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Appointment is not awaiting payment");
        }

        // Tự động gia hạn giữ chỗ thêm holdMinutes tính từ thời điểm bấm thanh toán
        Instant newExpiresAt = clock.instant().plus(Duration.ofMinutes(holdMinutes));
        appointment.setExpiresAt(newExpiresAt);
        appointmentRepository.save(appointment);

        return ChargeResponse.builder()
                .appointmentId(id)
                .patientId(appointment.getPatientId())
                .invoiceId(null)
                .type("EXAMINATION")
                .amount(appointment.getExaminationFee())
                .itemIds(List.of())
                .expiresAt(newExpiresAt)
                .build();
    }

    public void expireSession(Long id) {
        lockSession(id);
        expire(id);
    }

    private void expire(Long id) {
        Instant now = clock.instant();
        List<Appointment> expiredPending = appointmentRepository.findExpiredPendingAppointments(
                id, AppointmentStatus.PENDING_PAYMENT, now);

        for (Appointment appt : expiredPending) {
            try {
                ApiResponse<AppointmentPaymentCheckResponse> response = paymentClient.getExaminationPaymentStatus(appt.getId());
                AppointmentPaymentCheckResponse paymentInfo = response != null ? response.getResult() : null;

                if (paymentInfo != null && "PAID".equalsIgnoreCase(paymentInfo.getStatus())) {
                    log.info("Check before expire: Appointment #{} is already PAID. Confirming instead of expiring.", appt.getId());
                    PaymentConfirmedRequest confirmedRequest = PaymentConfirmedRequest.builder()
                            .appointmentId(appt.getId())
                            .invoiceId(paymentInfo.getInvoiceId())
                            .amount(paymentInfo.getAmount())
                            .paidAt(paymentInfo.getPaidAt())
                            .build();
                    confirmExamination(confirmedRequest);
                } else {
                    appt.setStatus(AppointmentStatus.EXPIRED);
                    appointmentRepository.save(appt);
                    log.info("Appointment #{} expired (unpaid).", appt.getId());
                }
            } catch (Exception e) {
                // Payment Service chết hoặc lỗi mạng -> Tạm hoãn hủy, gia hạn thêm 2 phút (Grace Period)
                log.warn("Cannot verify payment for appointment #{} (payment-service unavailable: {}). Postponing expiry by 2 minutes.",
                        appt.getId(), e.getMessage());
                appt.setExpiresAt(now.plus(Duration.ofMinutes(2)));
                appointmentRepository.save(appt);
            }
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneId.of(timezone)));
    }

    public Appointment find(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Appointment not found"));
    }

    public ExamSession lockSession(Long id) {
        return examSessionRepository.lockById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found"));
    }

    public Appointment lockAppointmentWithSession(Long id) {
        Long sessionId = appointmentRepository.findSessionId(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Appointment not found"));
        lockSession(sessionId);
        return appointmentRepository.lockById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Appointment not found"));
    }

    public void requireAssignedDoctor(Appointment appointment) {
        ExamSession session = examSessionRepository.findById(appointment.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found"));
        Access.require(Access.hasRole("DOCTOR") && session.getDoctorId().equals(Access.userId()));
    }

    /**
     * Đồng bộ bù hóa đơn tiền mặt cho lịch hẹn tiếp nhận tại quầy bị sót hóa đơn
     * (do sự cố mạng hoặc payment-service tạm thời gián đoạn lúc đăng ký tại quầy).
     */
    public Long reconcileCashInvoice(Long appointmentId) {
        Appointment apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Appointment not found"));

        if (apt.getExaminationInvoiceId() != null) {
            return apt.getExaminationInvoiceId();
        }

        if (apt.getExaminationFee() == null || apt.getExaminationFee().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        ApiResponse<Long> response = paymentClient.recordCashExamination(
                apt.getId(),
                apt.getPatientId(),
                apt.getExaminationFee()
        );

        if (response != null && response.getResult() != null) {
            apt.setExaminationInvoiceId(response.getResult());
            appointmentRepository.save(apt);
            log.info("Đã đồng bộ bù thành công hóa đơn cho lịch hẹn #{} -> invoiceId={}", apt.getId(), response.getResult());
            return response.getResult();
        }

        return null;
    }

    private void canRead(Appointment appointment, ExamSession session) {
        Access.require(Access.hasRole("ADMIN")
                || appointment.getPatientId().equals(Access.userId())
                || (Access.hasRole("DOCTOR") && session.getDoctorId().equals(Access.userId())));
    }
}

