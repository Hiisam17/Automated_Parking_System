package com.ktpm.parking.infrastructure.service;

import com.ktpm.parking.api.dto.PageMeta;
import com.ktpm.parking.api.dto.ParkingSpotPageResponse;
import com.ktpm.parking.api.dto.ParkingSpotResponse;
import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.business.domain.ParkingSpotStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.exception.BusinessException;
import com.ktpm.parking.business.exception.ResourceNotFoundException;
import com.ktpm.parking.business.repository.ParkingSpotRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Service tầng ứng dụng phục vụ các chức năng quản trị chỗ đỗ xe cho Admin.
 * Bao gồm: Xem/Lọc danh sách chỗ đỗ có phân trang, Bật bảo trì và Kết thúc bảo trì.
 * Thuộc phạm vi phụ trách của Member 3.
 */
@Service
public class ParkingSpotAdminService {

    private final ParkingSpotRepository spotRepository;

    public ParkingSpotAdminService(ParkingSpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    /**
     * Lấy danh sách chỗ đỗ xe có lọc theo điều kiện và phân trang.
     * Kết quả được sắp xếp theo code tăng dần (code ASC).
     *
     * @param status        lọc theo trạng thái AVAILABLE hoặc OCCUPIED (tùy chọn)
     * @param vehicleType   lọc theo loại xe MOTORBIKE hoặc CAR (tùy chọn)
     * @param inMaintenance lọc theo tình trạng bảo trì (tùy chọn)
     * @param page          chỉ số trang bắt đầu từ 0
     * @param size          kích thước trang (tối đa 100)
     * @return ParkingSpotPageResponse chứa danh sách chỗ đỗ và metadata phân trang
     */
    public ParkingSpotPageResponse listParkingSpots(
            ParkingSpotStatus status,
            VehicleType vehicleType,
            Boolean inMaintenance,
            int page,
            int size
    ) {
        // 1. Lấy toàn bộ danh sách chỗ đỗ và thực hiện lọc theo các tham số tùy chọn
        List<ParkingSpot> filteredSpots = spotRepository.findAll().stream()
                .filter(spot -> status == null || spot.occupancyStatus() == status)
                .filter(spot -> vehicleType == null || spot.vehicleType() == vehicleType)
                .filter(spot -> inMaintenance == null || spot.inMaintenance() == inMaintenance)
                .sorted(Comparator.comparing(ParkingSpot::code))
                .toList();

        long totalElements = filteredSpots.size();

        // 2. Tính toán phân trang
        int fromIndex = Math.min(page * size, (int) totalElements);
        int toIndex = Math.min(fromIndex + size, (int) totalElements);

        List<ParkingSpotResponse> items = filteredSpots.subList(fromIndex, toIndex).stream()
                .map(ParkingSpotResponse::fromDomain)
                .toList();

        PageMeta pageMeta = PageMeta.of(page, size, totalElements);
        return new ParkingSpotPageResponse(items, pageMeta);
    }

    /**
     * Bắt đầu bảo trì một chỗ đỗ xe.
     * Quy tắc nghiệp vụ:
     * - Chỗ đỗ phải tồn tại (nếu không -> 404 SPOT_NOT_FOUND).
     * - Chỉ được đưa vào bảo trì khi chỗ đang AVAILABLE (nếu đang OCCUPIED -> 409 SPOT_OCCUPIED).
     * - Không thể đưa vào bảo trì nếu chỗ đã đang bảo trì (-> 409 ALREADY_IN_MAINTENANCE).
     *
     * @param id ID của chỗ đỗ
     * @return ParkingSpotResponse thông tin chỗ đỗ sau khi cập nhật
     */
    public ParkingSpotResponse startMaintenance(Long id) {
        ParkingSpot spot = spotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SPOT_NOT_FOUND", "Không tìm thấy chỗ đỗ với ID: " + id));

        if (spot.occupancyStatus() == ParkingSpotStatus.OCCUPIED) {
            throw new BusinessException("SPOT_OCCUPIED", "Chỗ đang có xe đỗ, không thể đưa vào bảo trì.");
        }

        if (spot.inMaintenance()) {
            throw new BusinessException("ALREADY_IN_MAINTENANCE", "Chỗ đỗ này đã đang trong trạng thái bảo trì.");
        }

        ParkingSpot updatedSpot = new ParkingSpot(
                spot.id(),
                spot.code(),
                spot.vehicleType(),
                spot.occupancyStatus(),
                true
        );

        ParkingSpot saved = spotRepository.save(updatedSpot);
        return ParkingSpotResponse.fromDomain(saved);
    }

    /**
     * Kết thúc bảo trì và mở lại chỗ đỗ xe.
     * Đảm bảo tính idempotent: nếu chỗ vốn đã không bảo trì thì vẫn hoàn tất thành công.
     *
     * @param id ID của chỗ đỗ
     * @throws ResourceNotFoundException nếu không tìm thấy ID chỗ đỗ
     */
    public void endMaintenance(Long id) {
        ParkingSpot spot = spotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SPOT_NOT_FOUND", "Không tìm thấy chỗ đỗ với ID: " + id));

        if (spot.inMaintenance()) {
            ParkingSpot updatedSpot = new ParkingSpot(
                    spot.id(),
                    spot.code(),
                    spot.vehicleType(),
                    spot.occupancyStatus(),
                    false
            );
            spotRepository.save(updatedSpot);
        }
    }
}
