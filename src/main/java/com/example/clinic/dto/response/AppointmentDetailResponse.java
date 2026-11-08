package com.example.clinic.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDetailResponse {

    private AppointmentResponse appointment;
    private ExamSessionResponse session;
    private List<AppointmentServiceResponse> services;
    private List<AppointmentMedicationResponse> medications;
}
