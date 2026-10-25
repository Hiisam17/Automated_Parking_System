package com.ktpm.parking.infrastructure.service;

import com.ktpm.parking.api.dto.PageMeta;
import com.ktpm.parking.api.dto.ParkingSessionPageResponse;
import com.ktpm.parking.api.dto.ParkingSessionResponse;
import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.business.exception.ResourceNotFoundException;
import com.ktpm.parking.business.repository.ParkingSessionRepository;
import com.ktpm.parking.business.support.LicensePlateNormalizer;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Service tầng ứng dụng phục vụ các chức năng tra cứu phiên gửi xe cho Admin.
 * Bao gồm: Xem và lọc danh sách phiên gửi xe có phân trang và Xem chi tiết một phiên.
 */
@Service
public class ParkingSessionAdminService {

    private final ParkingSessionRepository sessionRepository;

    public ParkingSessionAdminService(ParkingSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Lấy danh sách các phiên gửi xe có lọc theo điều kiện và phân trang.
     * Sắp xếp mặc định: checkInAt giảm dần, sau đó id giảm dần.
     *
     * @param status       lọc theo trạng thái ACTIVE hoặc COMPLETED (tùy chọn)
     * @param licensePlate lọc theo biển số xe (tùy chọn, đối chiếu chuẩn hóa)
     * @param from         lọc checkInAt từ thời điểm này trở đi (bao gồm)
     * @param to           lọc checkInAt đến thời điểm này (bao gồm)
     * @param page         chỉ số trang bắt đầu từ 0
     * @param size         kích thước trang (1 đến 100)
     * @return ParkingSessionPageResponse danh sách các phiên và metadata phân trang
     * @throws IllegalArgumentException nếu from > to (trả về 400 VALIDATION_ERROR)
     */
    public ParkingSessionPageResponse listParkingSessions(
            ParkingSessionStatus status,
            String licensePlate,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int size
    ) {
        // 1. Ràng buộc nghiệp vụ: from phải nhỏ hơn hoặc bằng to
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Mốc thời gian 'from' phải nhỏ hơn hoặc bằng 'to'.");
        }

        // Chuẩn hóa biển số tìm kiếm nếu có
        String searchKey = (licensePlate != null && !licensePlate.isBlank())
                ? LicensePlateNormalizer.toKey(licensePlate)
                : null;

        // 2. Lấy toàn bộ danh sách phiên và lọc theo các tiêu chí tùy chọn
        List<ParkingSession> filteredSessions = sessionRepository.findAll().stream()
                .filter(s -> status == null || s.status() == status)
                .filter(s -> searchKey == null || LicensePlateNormalizer.toKey(s.licensePlate()).contains(searchKey))
                .filter(s -> from == null || !s.checkInTime().isBefore(from))
                .filter(s -> to == null || !s.checkInTime().isAfter(to))
                .sorted(Comparator
                        .comparing(ParkingSession::checkInTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ParkingSession::id, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();

        long totalElements = filteredSessions.size();

        // 3. Cắt dữ liệu theo trang yêu cầu (Pagination)
        int fromIndex = Math.min(page * size, (int) totalElements);
        int toIndex = Math.min(fromIndex + size, (int) totalElements);

        List<ParkingSessionResponse> items = filteredSessions.subList(fromIndex, toIndex).stream()
                .map(ParkingSessionResponse::fromDomain)
                .toList();

        PageMeta pageMeta = PageMeta.of(page, size, totalElements);
        return new ParkingSessionPageResponse(items, pageMeta);
    }

    /**
     * Tra cứu chi tiết một phiên gửi xe cụ thể theo ID.
     *
     * @param id ID của phiên gửi xe
     * @return ParkingSessionResponse thông tin chi tiết của phiên
     * @throws ResourceNotFoundException nếu không tìm thấy phiên (trả về 404 SESSION_NOT_FOUND)
     */
    public ParkingSessionResponse getParkingSession(Long id) {
        return sessionRepository.findById(id)
                .map(ParkingSessionResponse::fromDomain)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "Không tìm thấy phiên gửi xe với ID: " + id));
    }
}
