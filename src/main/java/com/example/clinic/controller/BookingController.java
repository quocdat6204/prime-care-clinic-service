package com.example.clinic.controller;

import com.example.clinic.dto.request.BookAppointmentRequest;
import com.example.clinic.dto.request.CreateSessionRequest;
import com.example.clinic.dto.request.WalkInAppointmentRequest;
import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.AppointmentDetailResponse;
import com.example.clinic.dto.response.AppointmentResponse;
import com.example.clinic.dto.response.ExamSessionResponse;
import com.example.clinic.service.BookingService;
import com.example.clinic.util.AppointmentStatus;
import com.example.clinic.util.SessionStatus;
import com.example.clinic.util.Shift;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller quản lý ca khám và lịch hẹn khám của bệnh nhân.
 */
@RestController
@RequestMapping("/clinic")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {

    BookingService bookingService;

    /**
     * Tạo ca khám bệnh mới cho bác sĩ (Dành cho ADMIN).
     *
     * @param request thông tin tạo ca khám
     * @return ca khám vừa được tạo
     */
    @PostMapping("/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ExamSessionResponse> createSession(@RequestBody @Valid CreateSessionRequest request) {
        return ApiResponse.<ExamSessionResponse>builder()
                .result(bookingService.createSession(request))
                .build();
    }

    /**
     * Lấy danh sách ca khám đang mở cho bệnh nhân đặt lịch theo ngày, ca khám và bác sĩ (tùy chọn), có phân trang.
     *
     * @param date ngày khám
     * @param doctorId mã định danh bác sĩ (tùy chọn)
     * @param shift ca khám (MORNING / AFTERNOON, tùy chọn)
     * @param pageable thông tin phân trang (page, size, sort)
     * @return trang danh sách ca khám mở kèm số chỗ còn trống
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('PATIENT','USER')")
    public ApiResponse<Page<ExamSessionResponse>> sessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Shift shift,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<ExamSessionResponse>>builder()
                .result(bookingService.patientSessions(date, doctorId, shift, pageable))
                .build();
    }



    /**
     * Lấy danh sách ca khám của bác sĩ hiện tại (truyền date để lọc theo ngày; không truyền thì lấy tất cả phân trang).
     *
     * @param date ngày khám (tùy chọn)
     * @param pageable thông tin phân trang
     * @return trang danh sách ca khám của bác sĩ
     */
    @GetMapping("/sessions/my")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<Page<ExamSessionResponse>> mySessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<ExamSessionResponse>>builder()
                .result(bookingService.doctorSessions(date, pageable))
                .build();
    }

    /**
     * Lấy danh sách ca khám dành cho Quản trị viên (hỗ trợ lọc linh hoạt theo ngày, bác sĩ, ca khám, trạng thái và phân trang).
     *
     * @param date ngày khám (tùy chọn)
     * @param doctorId mã bác sĩ (tùy chọn)
     * @param shift ca khám (tùy chọn)
     * @param status trạng thái ca khám (tùy chọn)
     * @param pageable thông tin phân trang
     * @return trang danh sách ca khám
     */
    @GetMapping("/sessions/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<ExamSessionResponse>> adminSessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Shift shift,
            @RequestParam(required = false) SessionStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<Page<ExamSessionResponse>>builder()
                .result(bookingService.adminSessions(date, doctorId, shift, status, pageable))
                .build();
    }



    /**
     * Bật / tắt trạng thái mở hoặc đóng ca khám (Dành cho ADMIN).
     * Cơ chế toggle: Nếu đang OPEN sẽ chuyển sang CLOSED, và ngược lại.
     *
     * @param id ID của ca khám
     * @return ca khám sau khi bật/tắt trạng thái
     */
    @PatchMapping(value = {"/sessions/{id}/status", "/sessions/{id}/toggle-status"})
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ExamSessionResponse> toggleStatus(@PathVariable Long id) {
        return ApiResponse.<ExamSessionResponse>builder()
                .result(bookingService.toggleSessionStatus(id))
                .build();
    }


    /**
     * Lấy danh sách lịch hẹn trong một ca khám (Dành cho ADMIN hoặc bác sĩ phụ trách).
     *
     * @param id ID của ca khám
     * @return danh sách lịch hẹn
     */
    @GetMapping("/sessions/{id}/appointments")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ApiResponse<List<AppointmentResponse>> sessionAppointments(
            @PathVariable Long id,
            @RequestParam(required = false) AppointmentStatus status) {
        return ApiResponse.<List<AppointmentResponse>>builder()
                .result(bookingService.sessionAppointments(id, status))
                .build();
    }

    /**
     * Tiếp nhận bệnh nhân khám trực tiếp tại quầy (Dành cho ADMIN).
     * Cho phép thêm bệnh nhân ngay cả khi ca khám đã hết slot.
     * Tự động tạo hóa đơn tiền mặt và chuyển trạng thái sang CONFIRMED.
     *
     * @param id ID của ca khám
     * @param request thông tin tiếp nhận tại quầy (patientId, symptoms)
     * @return lịch hẹn vừa được tiếp nhận và xác nhận
     */
    @PostMapping("/sessions/{id}/walk-in")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AppointmentResponse> walkIn(
            @PathVariable Long id,
            @RequestBody @Valid WalkInAppointmentRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .result(bookingService.walkIn(id, request))
                .build();
    }

    /**
     * Đặt lịch hẹn khám bệnh (Dành cho PATIENT hoặc USER).
     *
     * @param request thông tin đặt lịch
     * @return lịch hẹn vừa được tạo
     */
    @PostMapping("/appointments")
    @PreAuthorize("hasAnyRole('PATIENT','USER')")
    public ApiResponse<AppointmentResponse> book(@RequestBody @Valid BookAppointmentRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .result(bookingService.book(request))
                .build();
    }

    /**
     * Xem danh sách lịch hẹn cá nhân của người dùng hiện tại có phân trang.
     *
     * @param pageable thông tin phân trang
     * @return trang danh sách lịch hẹn
     */
    @GetMapping("/appointments/my")
    @PreAuthorize("hasAnyRole('PATIENT','USER')")
    public ApiResponse<Page<AppointmentResponse>> mine(Pageable pageable) {
        return ApiResponse.<Page<AppointmentResponse>>builder()
                .result(bookingService.mine(pageable))
                .build();
    }

    /**
     * Xem chi tiết lịch hẹn gồm ca khám, dịch vụ và đơn thuốc.
     *
     * @param id ID của lịch hẹn
     * @return thông tin chi tiết lịch hẹn
     */
    @GetMapping("/appointments/{id}")
    public ApiResponse<AppointmentDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.<AppointmentDetailResponse>builder()
                .result(bookingService.detail(id))
                .build();
    }

    /**
     * Hủy lịch hẹn khi chưa thanh toán (Dành cho người đặt lịch).
     *
     * @param id ID của lịch hẹn
     * @return lịch hẹn sau khi hủy
     */
    @PostMapping("/appointments/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','USER')")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable Long id) {
        return ApiResponse.<AppointmentResponse>builder()
                .result(bookingService.cancel(id))
                .build();
    }

    /**
     * Bác sĩ bắt đầu buổi khám bệnh cho lịch hẹn đã thanh toán.
     *
     * @param id ID của lịch hẹn
     * @return lịch hẹn chuyển sang trạng thái đang khám
     */
    @PostMapping("/appointments/{id}/start")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<AppointmentResponse> start(@PathVariable Long id) {
        return ApiResponse.<AppointmentResponse>builder()
                .result(bookingService.start(id))
                .build();
    }

    /**
     * Lấy danh sách lịch khám của một người dùng theo patientId (CHỈ DÀNH CHO ADMIN).
     *
     * @param patientId ID của bệnh nhân/người dùng
     * @return danh sách các lịch khám
     */
    @GetMapping("/appointments/patient/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AppointmentResponse>> getAppointmentsByPatient(@PathVariable Long patientId) {
        return ApiResponse.<List<AppointmentResponse>>builder()
                .result(bookingService.getAppointmentsByPatient(patientId))
                .build();
    }

    /**
     * Đồng bộ bù hóa đơn tiền mặt cho lịch hẹn nếu trước đó payment-service gặp sự cố gián đoạn.
     *
     * @param id ID của lịch hẹn
     * @return ID của hóa đơn vừa được đồng bộ
     */
    @PostMapping("/appointments/{id}/reconcile-invoice")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> reconcileInvoice(@PathVariable Long id) {
        return ApiResponse.<Long>builder()
                .result(bookingService.reconcileCashInvoice(id))
                .build();
    }
}
