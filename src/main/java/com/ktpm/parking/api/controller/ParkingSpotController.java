package com.ktpm.parking.api.controller;

import com.ktpm.parking.api.dto.ParkingSpotPageResponse;
import com.ktpm.parking.api.dto.ParkingSpotResponse;
import com.ktpm.parking.business.domain.ParkingSpotStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.service.ParkingSpotAdminService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller quản lý chỗ đỗ xe dành cho Quản trị viên (Admin).
 * Toàn bộ các API trong controller này yêu cầu xác thực JWT Bearer Token với quyền ROLE_ADMIN.
 */
@Validated
@RestController
@RequestMapping("/api/v1/parking-spots")
public class ParkingSpotController {

    private final ParkingSpotAdminService parkingSpotAdminService;

    public ParkingSpotController(ParkingSpotAdminService parkingSpotAdminService) {
        this.parkingSpotAdminService = parkingSpotAdminService;
    }

    /**
     * API Xem và lọc danh sách chỗ đỗ xe có phân trang.
     * Endpoint: GET /api/v1/parking-spots
     * Kết quả được sắp xếp tăng dần theo mã chỗ (code ASC).
     *
     * @param status        lọc theo trạng thái AVAILABLE hoặc OCCUPIED (tùy chọn)
     * @param vehicleType   lọc theo loại xe MOTORBIKE hoặc CAR (tùy chọn)
     * @param inMaintenance lọc theo tình trạng bảo trì (tùy chọn)
     * @param page          trang bắt đầu từ 0 (mặc định 0)
     * @param size          số bản ghi mỗi trang, tối đa 100 (mặc định 20)
     * @return HTTP 200 OK kèm ParkingSpotPageResponse
     */
    @GetMapping
    public ResponseEntity<ParkingSpotPageResponse> listParkingSpots(
            @RequestParam(required = false) ParkingSpotStatus status,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) Boolean inMaintenance,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        ParkingSpotPageResponse response = ParkingSpotPageResponse.from(parkingSpotAdminService.listParkingSpots(
                status,
                vehicleType,
                inMaintenance,
                page,
                size
        ));
        return ResponseEntity.ok(response);
    }

    /**
     * API Tạm ngừng chỗ đỗ đang trống để bảo trì.
     * Endpoint: POST /api/v1/parking-spots/{id}/maintenance
     * Không cần request body.
     *
     * @param id ID của chỗ đỗ cần bảo trì
     * @return HTTP 201 Created kèm thông tin chỗ đỗ (inMaintenance = true) nếu thành công;
     *         HTTP 404 SPOT_NOT_FOUND nếu không tìm thấy chỗ đỗ;
     *         HTTP 409 SPOT_OCCUPIED nếu chỗ đang có xe đỗ;
     *         HTTP 409 ALREADY_IN_MAINTENANCE nếu chỗ đã đang bảo trì.
     */
    @PostMapping("/{id}/maintenance")
    public ResponseEntity<ParkingSpotResponse> startMaintenance(@PathVariable @Min(1) Long id) {
        ParkingSpotResponse response = ParkingSpotResponse.fromDomain(parkingSpotAdminService.startMaintenance(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API Kết thúc bảo trì và mở lại chỗ đỗ xe.
     * Endpoint: DELETE /api/v1/parking-spots/{id}/maintenance
     * Không cần request body.
     * Thao tác mang tính idempotent: Nếu chỗ vốn đã không bảo trì thì vẫn trả về thành công.
     *
     * @param id ID của chỗ đỗ cần kết thúc bảo trì
     * @return HTTP 204 No Content nếu hoàn tất;
     *         HTTP 404 SPOT_NOT_FOUND nếu không tìm thấy chỗ đỗ.
     */
    @DeleteMapping("/{id}/maintenance")
    public ResponseEntity<Void> endMaintenance(@PathVariable @Min(1) Long id) {
        parkingSpotAdminService.endMaintenance(id);
        return ResponseEntity.noContent().build();
    }
}
