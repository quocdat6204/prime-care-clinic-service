package com.example.clinic.configuration;

import com.example.clinic.model.MedicalService;
import com.example.clinic.model.Medication;
import com.example.clinic.repository.MedicalServiceRepository;
import com.example.clinic.repository.MedicationRepository;
import com.example.clinic.util.ServiceType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Khởi tạo dữ liệu mẫu ban đầu cho dịch vụ khám và thuốc.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer implements CommandLineRunner {

    MedicalServiceRepository medicalServiceRepository;
    MedicationRepository medicationRepository;

    @Override
    public void run(String... args) {
        initMedicalServices();
        initMedications();
    }

    private void initMedicalServices() {
        // === 1. DỊCH VỤ XÉT NGHIỆM (LAB_TEST) ===
        createServiceIfAbsent(
                "Xét nghiệm công thức máu",
                ServiceType.LAB_TEST,
                new BigDecimal("150000"),
                "Phòng Xét nghiệm 101",
                "Phân tích các chỉ số hồng cầu, bạch cầu, tiểu cầu để phát hiện sớm các bệnh lý về máu hoặc tình trạng viêm nhiễm trước khi can thiệp nha khoa."
        );
        createServiceIfAbsent(
                "Xét nghiệm đường huyết",
                ServiceType.LAB_TEST,
                new BigDecimal("80000"),
                "Phòng Xét nghiệm 101",
                "Đo nồng độ đường trong máu nhằm sàng lọc và đảm bảo an toàn tối đa cho các thủ thuật nha khoa xâm lấn."
        );
        createServiceIfAbsent(
                "Xét nghiệm đông máu",
                ServiceType.LAB_TEST,
                new BigDecimal("180000"),
                "Phòng Xét nghiệm 101",
                "Kiểm tra các chỉ số đông máu (PT, APTT, INR), bắt buộc kiểm tra trước khi nhổ răng hoặc thực hiện tiểu phẫu thuật nha khoa."
        );

        // === 2. DỊCH VỤ CHẨN ĐOÁN HÌNH ẢNH (IMAGING) ===
        createServiceIfAbsent(
                "Chụp X-quang toàn cảnh (Panorama)",
                ServiceType.IMAGING,
                new BigDecimal("200000"),
                "Phòng Chẩn đoán hình ảnh 201",
                "Khảo sát toàn bộ xương hàm, vị trí răng mọc ngầm, răng khôn, khớp thái dương hàm và xoang hàm trên một góc nhìn tổng quan."
        );
        createServiceIfAbsent(
                "Chụp X-quang quanh chóp răng",
                ServiceType.IMAGING,
                new BigDecimal("80000"),
                "Phòng Chẩn đoán hình ảnh 201",
                "Chụp chi tiết thân và chóp chân răng nhằm phát hiện sâu kẽ, viêm cuống tủy hoặc tổn thương quanh chóp răng."
        );
        createServiceIfAbsent(
                "Chụp CT Cone Beam 3D",
                ServiceType.IMAGING,
                new BigDecimal("500000"),
                "Phòng Chụp CT 3D 202",
                "Dựng hình cắt lớp 3D độ nét cao khảo sát cấu trúc xương hàm, hệ thần kinh và xoang phục vụ cấy ghép Implant và nhổ răng khôn khó."
        );

        // === 3. THỦ THUẬT NHA KHOA (DENTAL) ===
        createServiceIfAbsent(
                "Lấy cao răng",
                ServiceType.DENTAL,
                new BigDecimal("250000"),
                "Phòng Khám & Thủ thuật",
                "Làm sạch vôi răng và mảng bám bằng sóng siêu âm hiện đại, đánh bóng bề mặt răng ngăn ngừa viêm nướu, viêm nha chu."
        );
        createServiceIfAbsent(
                "Bọc răng sứ",
                ServiceType.DENTAL,
                new BigDecimal("2500000"),
                "Phòng Khám & Thủ thuật",
                "Phục hình mão răng sứ thẩm mỹ cao, bảo vệ răng sâu vỡ lớn, sứt mẻ hoặc răng sau điều trị tủy."
        );
        createServiceIfAbsent(
                "Niềng răng mắc cài kim loại",
                ServiceType.DENTAL,
                new BigDecimal("25000000"),
                "Phòng Chỉnh nha",
                "Điều chỉnh khớp cắn, răng khấp khểnh, chen chúc, hô, móm bằng hệ thống mắc cài kim loại chuẩn y khoa."
        );
    }

    private void createServiceIfAbsent(String name, ServiceType type, BigDecimal price, String roomNumber, String description) {
        if (!medicalServiceRepository.existsByNameIgnoreCase(name)) {
            MedicalService service = MedicalService.builder()
                    .name(name)
                    .serviceType(type)
                    .price(price)
                    .roomNumber(roomNumber)
                    .description(description)
                    .active(true)
                    .build();
            medicalServiceRepository.save(service);
            log.info("Initialized default MedicalService [{}]: '{}'", type, name);
        }
    }

    private void initMedications() {
        // Thuốc 1: Paracetamol 500mg
        String m1Name = "Paracetamol 500mg";
        if (!medicationRepository.existsByName(m1Name)) {
            Medication m1 = Medication.builder()
                    .name(m1Name)
                    .strength("500mg")
                    .dosageForm("Viên nén")
                    .unit("Viên")
                    .description("Thuốc giảm đau, hạ sốt thông thường trong các trường hợp đau đầu, đau cơ, sốt do cảm cúm.")
                    .active(true)
                    .build();
            medicationRepository.save(m1);
            log.info("Initialized default Medication: '{}'", m1Name);
        }

        // Thuốc 2: Amoxicillin 500mg
        String m2Name = "Amoxicillin 500mg";
        if (!medicationRepository.existsByName(m2Name)) {
            Medication m2 = Medication.builder()
                    .name(m2Name)
                    .strength("500mg")
                    .dosageForm("Viên nang")
                    .unit("Viên")
                    .description("Kháng sinh nhóm aminopenicillin điều trị các bệnh lý nhiễm khuẩn đường hô hấp, tai mũi họng, tiết niệu.")
                    .active(true)
                    .build();
            medicationRepository.save(m2);
            log.info("Initialized default Medication: '{}'", m2Name);
        }

        // Thuốc 3: Omeprazole 20mg
        String m3Name = "Omeprazole 20mg";
        if (!medicationRepository.existsByName(m3Name)) {
            Medication m3 = Medication.builder()
                    .name(m3Name)
                    .strength("20mg")
                    .dosageForm("Viên nang tan trong ruột")
                    .unit("Viên")
                    .description("Thuốc ức chế bơm proton (PPI) điều trị viêm loét dạ dày - tá tràng và trào ngược dạ dày thực quản (GERD).")
                    .active(true)
                    .build();
            medicationRepository.save(m3);
            log.info("Initialized default Medication: '{}'", m3Name);
        }
    }
}
