package com.example.clinic.controller;

import com.example.clinic.dto.request.CompleteAppointmentRequest;
import com.example.clinic.dto.request.CompleteServiceRequest;
import com.example.clinic.dto.request.OrderServicesRequest;
import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.AppointmentResponse;
import com.example.clinic.dto.response.AppointmentServiceResponse;
import com.example.clinic.service.ClinicalService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các nghiệp vụ lâm sàng: chỉ định dịch vụ, thực hiện và hoàn tất khám.
 */
@RestController
@RequestMapping("/clinic")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClinicalController {

    ClinicalService clinicalService;

    /**
     * Bác sĩ chỉ định danh sách dịch vụ y tế cho lịch hẹn đang khám.
     *
     * @param id ID của lịch hẹn
     * @param request danh sách dịch vụ chỉ định
     * @return danh sách dịch vụ chỉ định đã tạo
     */
    @PostMapping("/appointments/{id}/services")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<List<AppointmentServiceResponse>> order(
            @PathVariable Long id,
            @RequestBody @Valid OrderServicesRequest request) {
        return ApiResponse.<List<AppointmentServiceResponse>>builder()
                .result(clinicalService.order(id, request))
                .build();
    }

    /**
     * Hủy dịch vụ chỉ định khi chưa tạo hóa đơn (Dành cho bác sĩ).
     *
     * @param id ID của dịch vụ chỉ định
     * @return dịch vụ chỉ định sau khi hủy
     */
    @PostMapping("/appointment-services/{id}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<AppointmentServiceResponse> cancel(@PathVariable Long id) {
        return ApiResponse.<AppointmentServiceResponse>builder()
                .result(clinicalService.cancelService(id))
                .build();
    }

    /**
     * Bắt đầu thực hiện dịch vụ chỉ định sau khi đã thanh toán.
     * Dành cho ADMIN/Kỹ thuật viên (với CLS) hoặc BÁC SĨ PHỤ TRÁCH (với thủ thuật điều trị).
     *
     * @param id ID của dịch vụ chỉ định
     * @return dịch vụ chuyển sang trạng thái đang thực hiện
     */
    @PostMapping("/appointment-services/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ApiResponse<AppointmentServiceResponse> start(@PathVariable Long id) {
        return ApiResponse.<AppointmentServiceResponse>builder()
                .result(clinicalService.startService(id))
                .build();
    }

    /**
     * Hoàn tất dịch vụ chỉ định và đính kèm kết quả/ghi chú.
     * Dành cho ADMIN/Kỹ thuật viên (với CLS) hoặc BÁC SĨ PHỤ TRÁCH (với thủ thuật điều trị).
     *
     * @param id ID của dịch vụ chỉ định
     * @param request kết quả dịch vụ và ghi chú
     * @return dịch vụ đã hoàn tất
     */
    @PostMapping("/appointment-services/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ApiResponse<AppointmentServiceResponse> complete(
            @PathVariable Long id,
            @RequestBody @Valid CompleteServiceRequest request) {
        return ApiResponse.<AppointmentServiceResponse>builder()
                .result(clinicalService.completeService(id, request))
                .build();
    }

    /**
     * Bác sĩ hoàn tất ca khám, chốt chẩn đoán, ghi chú và kê đơn thuốc.
     *
     * @param id ID của lịch hẹn
     * @param request kết quả chẩn đoán và danh sách thuốc kê đơn
     * @return lịch hẹn đã hoàn tất
     */
    @PostMapping("/appointments/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<AppointmentResponse> completeAppointment(
            @PathVariable Long id,
            @RequestBody @Valid CompleteAppointmentRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .result(clinicalService.completeAppointment(id, request))
                .build();
    }
}
