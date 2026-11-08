package com.example.clinic.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationResponse {

    private Long id;
    private Instant createdAt;
    private long version;
    private String name;
    private String strength;
    private String dosageForm;
    private String unit;
    private String description;
    private boolean active;
}
