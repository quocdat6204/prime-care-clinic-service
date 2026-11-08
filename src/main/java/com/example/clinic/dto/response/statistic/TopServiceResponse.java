package com.example.clinic.dto.response.statistic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopServiceResponse {

    List<ServiceStats> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStats {
        Long serviceId;
        String serviceName;
        String serviceType;
        long usageCount;
        long revenue;
    }
}
