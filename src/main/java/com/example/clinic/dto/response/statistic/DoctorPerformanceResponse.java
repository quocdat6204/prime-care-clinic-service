package com.example.clinic.dto.response.statistic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorPerformanceResponse {

    List<DoctorStats> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorStats {
        Long doctorId;
        String doctorName;
        String rank;
        long totalSessions;
        long totalAppointments;
        long completedAppointments;
        double avgPatientsPerSession;
        long revenue;
    }
}
