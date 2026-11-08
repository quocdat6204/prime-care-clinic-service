package com.example.clinic.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSummaryResponse {

    private Long id;
    private String fullName;
    private String rank;
    private String ava;
    private String email;
    private String phone;
}
