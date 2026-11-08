package com.example.clinic.service;

import com.example.clinic.client.UserClient;
import com.example.clinic.dto.response.DoctorSummaryResponse;
import com.example.clinic.dto.response.statistic.*;
import com.example.clinic.model.Appointment;
import com.example.clinic.model.AppointmentMedication;
import com.example.clinic.model.AppointmentService;
import com.example.clinic.model.ExamSession;
import com.example.clinic.repository.AppointmentMedicationRepository;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.AppointmentServiceRepository;
import com.example.clinic.repository.ExamSessionRepository;
import com.example.clinic.util.AppointmentStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class ClinicStatisticService {

    AppointmentRepository appointmentRepository;
    ExamSessionRepository examSessionRepository;
    AppointmentServiceRepository appointmentServiceRepository;
    AppointmentMedicationRepository appointmentMedicationRepository;
    UserClient userClient;

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private Instant parseStartOfDay(String dateStr) {
        return LocalDate.parse(dateStr).atStartOfDay(DEFAULT_ZONE).toInstant();
    }

    private Instant parseEndOfDay(String dateStr) {
        return LocalDate.parse(dateStr).plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
    }

    public AppointmentOverviewResponse getAppointmentOverview(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        Instant start = ym.atDay(1).atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        List<Appointment> list = appointmentRepository.findByCreatedAtBetween(start, end);
        long total = list.size();
        long completed = list.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long cancelled = list.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        long expired = list.stream().filter(a -> a.getStatus() == AppointmentStatus.EXPIRED).count();
        double completionRate = total > 0 ? Math.round(((double) completed / total * 100.0) * 100.0) / 100.0 : 0.0;

        return AppointmentOverviewResponse.builder()
                .total(total)
                .completed(completed)
                .cancelled(cancelled)
                .expired(expired)
                .completionRate(completionRate)
                .build();
    }

    public AppointmentStatsResponse getAppointmentStats(String from, String to, String groupBy) {
        Instant start = parseStartOfDay(from);
        Instant end = parseEndOfDay(to);

        List<Appointment> list = appointmentRepository.findByCreatedAtBetween(start, end);
        DateTimeFormatter formatter = "MONTH".equalsIgnoreCase(groupBy)
                ? DateTimeFormatter.ofPattern("yyyy-MM").withZone(DEFAULT_ZONE)
                : DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(DEFAULT_ZONE);

        Map<String, List<Appointment>> grouped = list.stream()
                .collect(Collectors.groupingBy(a -> formatter.format(a.getCreatedAt())));

        List<AppointmentStatsResponse.AppointmentStatsItem> items = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    String label = e.getKey();
                    List<Appointment> apps = e.getValue();
                    long total = apps.size();
                    long completed = apps.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
                    long cancelled = apps.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
                    long expired = apps.stream().filter(a -> a.getStatus() == AppointmentStatus.EXPIRED).count();
                    long other = total - completed - cancelled - expired;

                    return AppointmentStatsResponse.AppointmentStatsItem.builder()
                            .label(label)
                            .total(total)
                            .completed(completed)
                            .cancelled(cancelled)
                            .expired(expired)
                            .other(other)
                            .build();
                })
                .collect(Collectors.toList());

        return AppointmentStatsResponse.builder()
                .from(from)
                .to(to)
                .totalAppointments(list.size())
                .data(items)
                .build();
    }

    public AppointmentByShiftResponse getAppointmentsByShift(String from, String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        List<ExamSession> sessions = examSessionRepository.findByExamDateBetween(fromDate, toDate);
        if (sessions.isEmpty()) {
            return AppointmentByShiftResponse.builder().data(Collections.emptyList()).build();
        }

        Map<Long, ExamSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(ExamSession::getId, s -> s));

        List<Appointment> appointments = appointmentRepository.findBySessionIdIn(sessionMap.keySet());

        Map<String, List<ExamSession>> sessionsByShift = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getShift().name()));

        Map<String, List<Appointment>> appointmentsByShift = appointments.stream()
                .collect(Collectors.groupingBy(a -> {
                    ExamSession s = sessionMap.get(a.getSessionId());
                    return s != null ? s.getShift().name() : "UNKNOWN";
                }));

        List<AppointmentByShiftResponse.ShiftStats> list = new ArrayList<>();
        for (Map.Entry<String, List<ExamSession>> entry : sessionsByShift.entrySet()) {
            String shiftName = entry.getKey();
            long totalSessions = entry.getValue().size();
            List<Appointment> apps = appointmentsByShift.getOrDefault(shiftName, Collections.emptyList());
            long totalApps = apps.size();
            long completed = apps.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
            double avg = totalSessions > 0 ? Math.round(((double) totalApps / totalSessions) * 100.0) / 100.0 : 0.0;

            list.add(AppointmentByShiftResponse.ShiftStats.builder()
                    .shift(shiftName)
                    .totalSessions(totalSessions)
                    .totalAppointments(totalApps)
                    .completedAppointments(completed)
                    .avgPatientsPerSession(avg)
                    .build());
        }

        return AppointmentByShiftResponse.builder().data(list).build();
    }

    public DoctorPerformanceResponse getDoctorPerformance(String from, String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        List<ExamSession> sessions = examSessionRepository.findByExamDateBetween(fromDate, toDate);
        if (sessions.isEmpty()) {
            return DoctorPerformanceResponse.builder().data(Collections.emptyList()).build();
        }

        Map<Long, ExamSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(ExamSession::getId, s -> s));

        List<Appointment> appointments = appointmentRepository.findBySessionIdIn(sessionMap.keySet());

        Map<Long, List<ExamSession>> sessionsByDoctor = sessions.stream()
                .collect(Collectors.groupingBy(ExamSession::getDoctorId));

        Map<Long, List<Appointment>> appsByDoctor = appointments.stream()
                .collect(Collectors.groupingBy(a -> {
                    ExamSession s = sessionMap.get(a.getSessionId());
                    return s != null ? s.getDoctorId() : -1L;
                }));

        // Batch fetch doctor info from user-service
        List<Long> doctorIds = new ArrayList<>(sessionsByDoctor.keySet());
        Map<Long, DoctorSummaryResponse> doctorInfoMap = new HashMap<>();
        try {
            var response = userClient.getDoctorsBatch(doctorIds);
            if (response != null && response.getResult() != null) {
                response.getResult().forEach(d -> doctorInfoMap.put(d.getId(), d));
            }
        } catch (Exception e) {
            log.warn("Could not fetch doctor info from user-service: {}", e.getMessage());
        }

        List<DoctorPerformanceResponse.DoctorStats> resultList = new ArrayList<>();
        for (Long doctorId : doctorIds) {
            long totalSessions = sessionsByDoctor.get(doctorId).size();
            List<Appointment> apps = appsByDoctor.getOrDefault(doctorId, Collections.emptyList());
            long totalApps = apps.size();
            long completed = apps.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
            double avg = totalSessions > 0 ? Math.round(((double) totalApps / totalSessions) * 100.0) / 100.0 : 0.0;

            long revenue = apps.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED || a.getStatus() == AppointmentStatus.CONFIRMED)
                    .map(Appointment::getExaminationFee)
                    .filter(Objects::nonNull)
                    .mapToLong(BigDecimal::longValue)
                    .sum();

            DoctorSummaryResponse doc = doctorInfoMap.get(doctorId);
            String docName = doc != null && doc.getFullName() != null ? doc.getFullName() : "Bác sĩ #" + doctorId;
            String rank = doc != null && doc.getRank() != null ? doc.getRank() : "N/A";

            resultList.add(DoctorPerformanceResponse.DoctorStats.builder()
                    .doctorId(doctorId)
                    .doctorName(docName)
                    .rank(rank)
                    .totalSessions(totalSessions)
                    .totalAppointments(totalApps)
                    .completedAppointments(completed)
                    .avgPatientsPerSession(avg)
                    .revenue(revenue)
                    .build());
        }

        // Sắp xếp theo số lượt khám giảm dần
        resultList.sort(Comparator.comparingLong(DoctorPerformanceResponse.DoctorStats::getTotalAppointments).reversed());

        return DoctorPerformanceResponse.builder().data(resultList).build();
    }

    public RevenueByServiceTypeResponse getRevenueByServiceType(String from, String to) {
        Instant start = parseStartOfDay(from);
        Instant end = parseEndOfDay(to);

        List<AppointmentService> services = appointmentServiceRepository.findByCreatedAtBetween(start, end);
        long grandTotal = services.stream()
                .map(AppointmentService::getUnitPrice)
                .filter(Objects::nonNull)
                .mapToLong(BigDecimal::longValue)
                .sum();

        Map<String, List<AppointmentService>> grouped = services.stream()
                .collect(Collectors.groupingBy(s -> s.getServiceType() != null ? s.getServiceType().name() : "OTHER"));

        List<RevenueByServiceTypeResponse.ServiceTypeRevenue> items = new ArrayList<>();
        for (Map.Entry<String, List<AppointmentService>> entry : grouped.entrySet()) {
            long rev = entry.getValue().stream()
                    .map(AppointmentService::getUnitPrice)
                    .filter(Objects::nonNull)
                    .mapToLong(BigDecimal::longValue)
                    .sum();
            double pct = grandTotal > 0 ? Math.round(((double) rev / grandTotal * 100.0) * 100.0) / 100.0 : 0.0;

            items.add(RevenueByServiceTypeResponse.ServiceTypeRevenue.builder()
                    .serviceType(entry.getKey())
                    .count(entry.getValue().size())
                    .revenue(rev)
                    .percentage(pct)
                    .build());
        }

        items.sort(Comparator.comparingLong(RevenueByServiceTypeResponse.ServiceTypeRevenue::getRevenue).reversed());

        return RevenueByServiceTypeResponse.builder()
                .total(grandTotal)
                .data(items)
                .build();
    }

    public TopServiceResponse getTopServices(String from, String to, int limit) {
        Instant start = parseStartOfDay(from);
        Instant end = parseEndOfDay(to);

        List<AppointmentService> services = appointmentServiceRepository.findByCreatedAtBetween(start, end);

        Map<Long, List<AppointmentService>> grouped = services.stream()
                .collect(Collectors.groupingBy(AppointmentService::getServiceId));

        List<TopServiceResponse.ServiceStats> stats = grouped.entrySet().stream()
                .map(entry -> {
                    Long sId = entry.getKey();
                    List<AppointmentService> list = entry.getValue();
                    String name = list.isEmpty() ? "" : list.get(0).getServiceName();
                    String type = list.isEmpty() || list.get(0).getServiceType() == null ? "" : list.get(0).getServiceType().name();
                    long rev = list.stream()
                            .map(AppointmentService::getUnitPrice)
                            .filter(Objects::nonNull)
                            .mapToLong(BigDecimal::longValue)
                            .sum();

                    return TopServiceResponse.ServiceStats.builder()
                            .serviceId(sId)
                            .serviceName(name)
                            .serviceType(type)
                            .usageCount(list.size())
                            .revenue(rev)
                            .build();
                })
                .sorted(Comparator.comparingLong(TopServiceResponse.ServiceStats::getUsageCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        return TopServiceResponse.builder().data(stats).build();
    }

    public TopMedicationResponse getTopMedications(String from, String to, int limit) {
        Instant start = parseStartOfDay(from);
        Instant end = parseEndOfDay(to);

        List<AppointmentMedication> meds = appointmentMedicationRepository.findByCreatedAtBetween(start, end);

        Map<Long, List<AppointmentMedication>> grouped = meds.stream()
                .collect(Collectors.groupingBy(AppointmentMedication::getMedicationId));

        List<TopMedicationResponse.MedicationStats> stats = grouped.entrySet().stream()
                .map(entry -> {
                    Long mId = entry.getKey();
                    List<AppointmentMedication> list = entry.getValue();
                    String name = list.isEmpty() ? "" : list.get(0).getMedicineName();
                    String strength = list.isEmpty() ? "" : list.get(0).getStrength();
                    long totalQty = list.stream().mapToLong(AppointmentMedication::getQuantity).sum();

                    return TopMedicationResponse.MedicationStats.builder()
                            .medicationId(mId)
                            .medicineName(name)
                            .strength(strength)
                            .prescriptionCount(list.size())
                            .totalQuantity(totalQty)
                            .build();
                })
                .sorted(Comparator.comparingLong(TopMedicationResponse.MedicationStats::getPrescriptionCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        return TopMedicationResponse.builder().data(stats).build();
    }
}
