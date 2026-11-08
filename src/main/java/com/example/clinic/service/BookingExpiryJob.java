package com.example.clinic.service;

import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.util.AppointmentStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Job tự động quét và hủy các lịch hẹn đang chờ thanh toán nhưng đã quá hạn giữ chỗ.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(name = "clinic.expiry-enabled", havingValue = "true", matchIfMissing = true)
public class BookingExpiryJob {

    AppointmentRepository appointmentRepository;
    BookingService bookingService;
    Clock clock;

    @Scheduled(fixedDelayString = "${clinic.expiry-interval-ms:60000}")
    public void expireBookings() {
        appointmentRepository.findExpiredSessionIds(AppointmentStatus.PENDING_PAYMENT, clock.instant())
                .forEach(bookingService::expireSession);
    }
}
