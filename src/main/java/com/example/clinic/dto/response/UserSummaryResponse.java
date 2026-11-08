package com.example.clinic.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String gender;
    private String ava;
}

