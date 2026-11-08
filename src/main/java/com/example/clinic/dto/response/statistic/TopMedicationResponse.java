package com.example.clinic.dto.response.statistic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopMedicationResponse {

    List<MedicationStats> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationStats {
        Long medicationId;
        String medicineName;
        String strength;
        long prescriptionCount;
        long totalQuantity;
    }
}
