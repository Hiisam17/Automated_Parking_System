package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.business.domain.ParkingSpotStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.exception.BusinessException;
import com.ktpm.parking.business.exception.ResourceNotFoundException;
import com.ktpm.parking.business.repository.ParkingSpotRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ParkingSpotAdminService {

    private final ParkingSpotRepository spots;

    public ParkingSpotAdminService(ParkingSpotRepository spots) {
        this.spots = Objects.requireNonNull(spots);
    }

    public PageResult<ParkingSpot> listParkingSpots(ParkingSpotStatus status,
                                                    VehicleType vehicleType,
                                                    Boolean inMaintenance,
                                                    int page,
                                                    int size) {
        validatePage(page, size);
        List<ParkingSpot> filtered = spots.findAll().stream()
                .filter(spot -> status == null || spot.occupancyStatus() == status)
                .filter(spot -> vehicleType == null || spot.vehicleType() == vehicleType)
                .filter(spot -> inMaintenance == null || spot.inMaintenance() == inMaintenance)
                .sorted(Comparator.comparing(ParkingSpot::code))
                .toList();

        return page(filtered, page, size);
    }

    public ParkingSpot startMaintenance(Long id) {
        ParkingSpot spot = findRequired(id);
        if (spot.occupancyStatus() == ParkingSpotStatus.OCCUPIED) {
            throw new BusinessException("SPOT_OCCUPIED", "Chỗ đang có xe đỗ, không thể đưa vào bảo trì.");
        }
        if (spot.inMaintenance()) {
            throw new BusinessException("ALREADY_IN_MAINTENANCE", "Chỗ đỗ này đã đang trong trạng thái bảo trì.");
        }

        return spots.save(new ParkingSpot(
                spot.id(), spot.code(), spot.vehicleType(), spot.occupancyStatus(), true
        ));
    }

    public void endMaintenance(Long id) {
        ParkingSpot spot = findRequired(id);
        if (spot.inMaintenance()) {
            spots.save(new ParkingSpot(
                    spot.id(), spot.code(), spot.vehicleType(), spot.occupancyStatus(), false
            ));
        }
    }

    private ParkingSpot findRequired(Long id) {
        return spots.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                "SPOT_NOT_FOUND", "Không tìm thấy chỗ đỗ với ID: " + id
        ));
    }

    private static <T> PageResult<T> page(List<T> items, int page, int size) {
        long total = items.size();
        int from = (int) Math.min((long) page * size, total);
        int to = (int) Math.min((long) from + size, total);
        return new PageResult<>(items.subList(from, to), page, size, total);
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Tham số phân trang không hợp lệ.");
        }
    }
}
