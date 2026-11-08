package com.example.clinic.dto.response.statistic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppointmentStatsResponse {

    String from;
    String to;
    long totalAppointments;
    List<AppointmentStatsItem> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentStatsItem {
        String label;
        long total;
        long completed;
        long cancelled;
        long expired;
        long other;
    }
}
