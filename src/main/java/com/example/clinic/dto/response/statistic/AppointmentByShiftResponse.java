package com.example.clinic.dto.response.statistic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppointmentByShiftResponse {

    List<ShiftStats> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftStats {
        String shift;
        long totalSessions;
        long totalAppointments;
        long completedAppointments;
        double avgPatientsPerSession;
    }
}
