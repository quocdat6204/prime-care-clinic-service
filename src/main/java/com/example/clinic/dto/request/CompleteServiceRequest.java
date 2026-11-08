package com.example.clinic.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteServiceRequest {

    @Size(max = 2048, message = "Result URL must not exceed 2048 characters")
    private String resultUrl;

    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;
}
