package com.example.clinic.controller;

import com.example.clinic.dto.request.BindServiceInvoiceRequest;
import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.AppointmentServiceResponse;
import com.example.clinic.dto.response.ChargeResponse;
import com.example.clinic.service.BookingService;
import com.example.clinic.service.ClinicalService;
import jakarta.validation.Valid;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

/**
 * Controller nội bộ phục vụ giao tiếp giữa Payment Service và Clinic Service qua OpenFeign.
 */
@RestController
@RequestMapping("/internal/clinic")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalPaymentController {

    BookingService bookingService;
    ClinicalService clinicalService;

    /**
     * Lấy thông tin thanh toán phí khám của lịch hẹn.
     *
     * @param id ID của lịch hẹn
     * @return thông tin chi phí khám bệnh
     */
    @GetMapping("/appointments/{id}/examination-charge")
    public ApiResponse<ChargeResponse> examinationCharge(@PathVariable Long id) {
        return ApiResponse.<ChargeResponse>builder()
                .result(bookingService.examinationCharge(id))
                .build();
    }

    /**
     * Gắn mã hóa đơn vào nhóm dịch vụ chỉ định cần thanh toán và trả về tổng tiền.
     *
     * @param id ID của lịch hẹn
     * @param request thông tin mã hóa đơn và danh sách dịch vụ
     * @return thông tin chi phí của nhóm dịch vụ
     */
    @PostMapping("/appointments/{id}/service-invoices")
    public ApiResponse<ChargeResponse> bind(
            @PathVariable Long id,
            @RequestBody @Valid BindServiceInvoiceRequest request) {
        return ApiResponse.<ChargeResponse>builder()
                .result(clinicalService.bindServiceInvoice(id, request))
                .build();
    }

    /**
     * Lấy danh sách toàn bộ dịch vụ của một lịch hẹn (phục vụ payment-service hiển thị chi tiết hóa đơn).
     *
     * @param id ID của lịch hẹn
     * @return danh sách dịch vụ y tế của lịch hẹn
     */
    @GetMapping("/appointments/{id}/services")
    public ApiResponse<List<AppointmentServiceResponse>> getAppointmentServices(@PathVariable Long id) {
        return ApiResponse.<List<AppointmentServiceResponse>>builder()
                .result(clinicalService.getServicesByAppointment(id))
                .build();
    }
}
