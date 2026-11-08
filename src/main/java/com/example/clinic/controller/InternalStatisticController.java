package com.example.clinic.controller;

import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.statistic.*;
import com.example.clinic.service.ClinicStatisticService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller nội bộ phục vụ statistic-service gọi qua OpenFeign.
 */
@RestController
@RequestMapping("/internal/clinic/statistics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalStatisticController {

    ClinicStatisticService clinicStatisticService;

    @GetMapping("/appointment-overview")
    public ApiResponse<AppointmentOverviewResponse> getAppointmentOverview(
            @RequestParam int month,
            @RequestParam int year) {
        return ApiResponse.<AppointmentOverviewResponse>builder()
                .result(clinicStatisticService.getAppointmentOverview(month, year))
                .build();
    }

    @GetMapping("/appointments")
    public ApiResponse<AppointmentStatsResponse> getAppointmentStats(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String groupBy) {
        return ApiResponse.<AppointmentStatsResponse>builder()
                .result(clinicStatisticService.getAppointmentStats(from, to, groupBy))
                .build();
    }

    @GetMapping("/appointments/by-shift")
    public ApiResponse<AppointmentByShiftResponse> getAppointmentsByShift(
            @RequestParam String from,
            @RequestParam String to) {
        return ApiResponse.<AppointmentByShiftResponse>builder()
                .result(clinicStatisticService.getAppointmentsByShift(from, to))
                .build();
    }

    @GetMapping("/doctors/performance")
    public ApiResponse<DoctorPerformanceResponse> getDoctorPerformance(
            @RequestParam String from,
            @RequestParam String to) {
        return ApiResponse.<DoctorPerformanceResponse>builder()
                .result(clinicStatisticService.getDoctorPerformance(from, to))
                .build();
    }

    @GetMapping("/revenue/by-service-type")
    public ApiResponse<RevenueByServiceTypeResponse> getRevenueByServiceType(
            @RequestParam String from,
            @RequestParam String to) {
        return ApiResponse.<RevenueByServiceTypeResponse>builder()
                .result(clinicStatisticService.getRevenueByServiceType(from, to))
                .build();
    }

    @GetMapping("/services/top")
    public ApiResponse<TopServiceResponse> getTopServices(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam int limit) {
        return ApiResponse.<TopServiceResponse>builder()
                .result(clinicStatisticService.getTopServices(from, to, limit))
                .build();
    }

    @GetMapping("/medications/top")
    public ApiResponse<TopMedicationResponse> getTopMedications(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam int limit) {
        return ApiResponse.<TopMedicationResponse>builder()
                .result(clinicStatisticService.getTopMedications(from, to, limit))
                .build();
    }
}
