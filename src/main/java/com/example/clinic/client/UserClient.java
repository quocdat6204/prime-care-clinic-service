package com.example.clinic.client;

import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.DoctorSummaryResponse;
import com.example.clinic.dto.response.UserSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @PostMapping("/users/internal/doctors/batch")
    ApiResponse<List<DoctorSummaryResponse>> getDoctorsBatch(@RequestBody List<Long> ids);

    @PostMapping("/users/internal/users/batch")
    ApiResponse<List<UserSummaryResponse>> getUsersBatch(@RequestBody List<Long> ids);
}
