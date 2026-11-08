package com.example.clinic.consumer;

import com.example.clinic.dto.event.PaymentSuccessEvent;
import com.example.clinic.dto.request.PaymentConfirmedRequest;
import com.example.clinic.service.BookingService;
import com.example.clinic.service.ClinicalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentEventConsumer {

    BookingService bookingService;
    ClinicalService clinicalService;

    @KafkaListener(
            topics = "${payment.kafka.topic.payment-events:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:clinic-service-group}"
    )
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent from Kafka: invoiceId={}, appointmentId={}, type={}, amount={}",
                event.getInvoiceId(), event.getAppointmentId(), event.getType(), event.getAmount());

        try {
            PaymentConfirmedRequest confirmedRequest = PaymentConfirmedRequest.builder()
                    .appointmentId(event.getAppointmentId())
                    .invoiceId(event.getInvoiceId())
                    .amount(event.getAmount())
                    .paidAt(event.getPaidAt())
                    .build();

            if ("EXAMINATION".equalsIgnoreCase(event.getType())) {
                bookingService.confirmExamination(confirmedRequest);
                log.info("Successfully confirmed examination payment for appointmentId={}, invoiceId={}",
                        event.getAppointmentId(), event.getInvoiceId());
            } else if ("SERVICES".equalsIgnoreCase(event.getType())) {
                clinicalService.confirmServices(confirmedRequest);
                log.info("Successfully confirmed clinical services payment for appointmentId={}, invoiceId={}",
                        event.getAppointmentId(), event.getInvoiceId());
            } else {
                log.warn("Unknown invoice type received in PaymentSuccessEvent: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Failed to process PaymentSuccessEvent for invoiceId={}: {}", event.getInvoiceId(), e.getMessage(), e);
            throw e;
        }
    }
}
