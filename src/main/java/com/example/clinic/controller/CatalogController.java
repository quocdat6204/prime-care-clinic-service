package com.example.clinic.controller;

import com.example.clinic.dto.request.MedicationInputRequest;
import com.example.clinic.dto.request.ServiceInputRequest;
import com.example.clinic.dto.response.ApiResponse;
import com.example.clinic.dto.response.MedicalServiceResponse;
import com.example.clinic.dto.response.MedicationResponse;
import com.example.clinic.security.Access;
import com.example.clinic.service.CatalogService;
import com.example.clinic.util.ServiceType;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý danh mục dịch vụ khám và thuốc.
 */
@RestController
@RequestMapping("/clinic")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogController {

    CatalogService catalogService;

    /**
     * Lấy danh sách dịch vụ y tế có phân trang (Dành cho ADMIN và DOCTOR).
     * Bác sĩ chỉ được xem dịch vụ đang active; có thể lọc theo loại dịch vụ (serviceType) và từ khóa (keyword).
     *
     * @param type loại dịch vụ y tế (tùy chọn: LAB_TEST, IMAGING, DENTAL, OTHER)
     * @param serviceType loại dịch vụ y tế (alias của type)
     * @param keyword từ khóa tìm kiếm theo tên hoặc mô tả dịch vụ (tùy chọn)
     * @param includeInactive có bao gồm dịch vụ ngưng hoạt động hay không (chỉ ADMIN)
     * @param pageable thông tin phân trang
     * @return trang danh sách dịch vụ
     */
    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ApiResponse<Page<MedicalServiceResponse>> services(
            @RequestParam(required = false) ServiceType type,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(sort = "id") Pageable pageable) {
        if (includeInactive) {
            Access.require(Access.hasRole("ADMIN"));
        }
        ServiceType filterType = serviceType != null ? serviceType : type;
        return ApiResponse.<Page<MedicalServiceResponse>>builder()
                .result(catalogService.services(filterType, keyword, includeInactive, pageable))
                .build();
    }

    /**
     * Bật / tắt trạng thái hoạt động của dịch vụ y tế (Dành cho ADMIN).
     * Cơ chế toggle: Nếu active đang là true sẽ chuyển sang false, và ngược lại.
     *
     * @param id ID của dịch vụ y tế
     * @return dịch vụ y tế sau khi đổi trạng thái
     */
    @PatchMapping(value = {"/services/{id}/status", "/services/{id}/toggle-status"})
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MedicalServiceResponse> toggleServiceStatus(@PathVariable Long id) {
        return ApiResponse.<MedicalServiceResponse>builder()
                .result(catalogService.toggleServiceStatus(id))
                .build();
    }

    /**
     * Thêm mới dịch vụ y tế vào danh mục (Dành cho ADMIN).
     *
     * @param request thông tin dịch vụ y tế mới
     * @return dịch vụ y tế vừa được tạo
     */
    @PostMapping("/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MedicalServiceResponse> createService(@RequestBody @Valid ServiceInputRequest request) {
        return ApiResponse.<MedicalServiceResponse>builder()
                .result(catalogService.saveService(null, request))
                .build();
    }

    /**
     * Cập nhật thông tin dịch vụ y tế (Dành cho ADMIN).
     *
     * @param id ID của dịch vụ cần cập nhật
     * @param request thông tin cập nhật
     * @return dịch vụ y tế sau khi cập nhật
     */
    @PutMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MedicalServiceResponse> updateService(
            @PathVariable Long id,
            @RequestBody @Valid ServiceInputRequest request) {
        return ApiResponse.<MedicalServiceResponse>builder()
                .result(catalogService.saveService(id, request))
                .build();
    }

    /**
     * Xóa dịch vụ y tế khỏi danh mục (Dành cho ADMIN).
     *
     * @param id ID của dịch vụ cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteService(@PathVariable Long id) {
        catalogService.deleteService(id);
        return ApiResponse.<Void>builder()
                .message("Service deleted successfully")
                .build();
    }

    /**
     * Lấy danh sách thuốc có phân trang (Dành cho ADMIN và DOCTOR).
     * Bác sĩ chỉ được xem thuốc đang active; có thể tìm kiếm theo từ khóa tên hoặc mô tả (keyword).
     *
     * @param keyword từ khóa tìm kiếm theo tên hoặc mô tả (tùy chọn)
     * @param includeInactive có bao gồm thuốc ngưng hoạt động hay không (chỉ ADMIN)
     * @param pageable thông tin phân trang (mặc định sort = name)
     * @return trang danh sách thuốc
     */
    @GetMapping("/medications")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ApiResponse<Page<MedicationResponse>> medications(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(sort = "id") Pageable pageable) {
        if (includeInactive) {
            Access.require(Access.hasRole("ADMIN"));
        }
        return ApiResponse.<Page<MedicationResponse>>builder()
                .result(catalogService.medications(keyword, includeInactive, pageable))
                .build();
    }

    /**
     * Bật / tắt trạng thái hoạt động của thuốc (Dành cho ADMIN).
     * Cơ chế toggle: Nếu active đang là true sẽ chuyển sang false, và ngược lại.
     *
     * @param id ID của thuốc
     * @return thuốc sau khi đổi trạng thái
     */
    @PatchMapping(value = {"/medications/{id}/status", "/medications/{id}/toggle-status"})
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MedicationResponse> toggleMedicationStatus(@PathVariable Long id) {
        return ApiResponse.<MedicationResponse>builder()
                .result(catalogService.toggleMedicationStatus(id))
                .build();
    }

    /**
     * Thêm mới thuốc vào danh mục (Dành cho ADMIN).
     *
     * @param request thông tin thuốc mới
     * @return thuốc vừa được tạo
     */
    @PostMapping("/medications")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MedicationResponse> createMedication(@RequestBody @Valid MedicationInputRequest request) {
        return ApiResponse.<MedicationResponse>builder()
                .result(catalogService.saveMedication(null, request))
                .build();
    }

    /**
     * Cập nhật thông tin thuốc trong danh mục (Dành cho ADMIN).
     *
     * @param id ID của thuốc cần cập nhật
     * @param request thông tin cập nhật
     * @return thuốc sau khi cập nhật
     */
    @PutMapping("/medications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MedicationResponse> updateMedication(
            @PathVariable Long id,
            @RequestBody @Valid MedicationInputRequest request) {
        return ApiResponse.<MedicationResponse>builder()
                .result(catalogService.saveMedication(id, request))
                .build();
    }

    /**
     * Xóa thuốc khỏi danh mục (Dành cho ADMIN).
     *
     * @param id ID của thuốc cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/medications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteMedication(@PathVariable Long id) {
        catalogService.deleteMedication(id);
        return ApiResponse.<Void>builder()
                .message("Medication deleted successfully")
                .build();
    }
}
