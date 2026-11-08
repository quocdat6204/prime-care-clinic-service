package com.example.clinic.service;

import com.example.clinic.model.Appointment;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.util.AppointmentStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Background Job tự động quét và bù hóa đơn tiền mặt cho các ca tiếp nhận tại quầy
 * trong trường hợp payment-service bị gián đoạn / lỗi mạng lúc tạo lịch hẹn.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@ConditionalOnProperty(name = "clinic.reconcile-invoices-enabled", havingValue = "true", matchIfMissing = true)
public class CashInvoiceReconciliationJob {

    AppointmentRepository appointmentRepository;
    BookingService bookingService;

    static final List<AppointmentStatus> RECONCILE_STATUSES = List.of(
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.EXAMINING,
            AppointmentStatus.COMPLETED
    );

    @Scheduled(fixedDelayString = "${clinic.reconcile-interval-ms:30000}")
    public void reconcileMissingCashInvoices() {
        List<Appointment> missing = appointmentRepository.findMissingExaminationInvoices(RECONCILE_STATUSES);
        if (missing.isEmpty()) {
            return;
        }

        log.info("Phát hiện {} lịch hẹn khám chưa có hóa đơn tiền mặt, đang tiến hành đồng bộ bù...", missing.size());

        for (Appointment apt : missing) {
            try {
                Long invoiceId = bookingService.reconcileCashInvoice(apt.getId());
                if (invoiceId != null) {
                    log.info("Đã đồng bộ bù thành công hóa đơn tiền mặt cho lịch hẹn #{} -> invoiceId={}",
                            apt.getId(), invoiceId);
                }
            } catch (Exception e) {
                log.warn("Không thể đồng bộ bù hóa đơn cho lịch hẹn #{} (payment-service có thể chưa sẵn sàng): {}",
                        apt.getId(), e.getMessage());
                // Ngắt vòng lặp hiện tại để không lặp lại lỗi khi payment-service đang tạm thời ngắt kết nối
                break;
            }
        }
    }
}

