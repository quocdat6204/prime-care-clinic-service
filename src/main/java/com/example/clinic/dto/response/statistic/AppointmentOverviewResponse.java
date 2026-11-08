package com.example.clinic.dto.response.statistic;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppointmentOverviewResponse {
    long total;
    long completed;
    long cancelled;
    long expired;
    double completionRate;
}
