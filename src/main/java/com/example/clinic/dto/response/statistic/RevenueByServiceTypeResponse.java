package com.example.clinic.dto.response.statistic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueByServiceTypeResponse {

    long total;
    List<ServiceTypeRevenue> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceTypeRevenue {
        String serviceType;
        long count;
        long revenue;
        double percentage;
    }
}
