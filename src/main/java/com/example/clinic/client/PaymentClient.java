package com.example.clinic.client;

import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.AppointmentPaymentCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @GetMapping("/internal/payment/appointments/{appointmentId}/examination-status")
    ApiResponse<AppointmentPaymentCheckResponse> getExaminationPaymentStatus(
            @PathVariable("appointmentId") Long appointmentId);

    @PostMapping("/internal/payment/appointments/{appointmentId}/cash-examination")
    ApiResponse<Long> recordCashExamination(
            @PathVariable("appointmentId") Long appointmentId,
            @RequestParam("patientId") Long patientId,
            @RequestParam("amount") BigDecimal amount);
}

