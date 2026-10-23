package com.ktpm.parking.api.controller;

import com.ktpm.parking.api.dto.EntryRequest;
import com.ktpm.parking.api.dto.EntryResponse;
import com.ktpm.parking.api.dto.ExitRequest;
import com.ktpm.parking.api.dto.ExitResponse;
import com.ktpm.parking.api.error.ApiErrorResponse;
import com.ktpm.parking.business.service.CheckInCommand;
import com.ktpm.parking.business.service.CheckOutCommand;
import com.ktpm.parking.business.service.GateDecision;
import com.ktpm.parking.business.service.GateResult;
import com.ktpm.parking.business.service.ParkingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller tiếp nhận sự kiện tự động từ thiết bị cổng mô phỏng (Gate Simulator).
 * Các API này yêu cầu xác thực bằng header X-Gate-Api-Key (quyền ROLE_GATE).
 * Thuộc phạm vi phụ trách của Member 3 (kết nối với ParkingService của Member 1).
 */
@RestController
@RequestMapping("/api/v1/gates")
public class GateController {

    // Tên header thông báo sự kiện gửi lại (idempotency replay) theo đặc tả OpenAPI
    public static final String HEADER_EVENT_REPLAYED = "X-Event-Replayed";

    private final ParkingService parkingService;

    public GateController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /**
     * API Xe vào bãi — Tiếp nhận sự kiện check-in tự động.
     * Endpoint: POST /api/v1/gates/entry
     *
     * @param request dữ liệu cổng vào (eventId, gateId, licensePlate, vehicleType)
     * @return HTTP 201 Created kèm EntryResponse nếu thành công;
     *         HTTP 409 Conflict kèm ApiErrorResponse nếu vi phạm nghiệp vụ (hết chỗ, xe đã ở trong bãi,...);
     *         Header X-Event-Replayed: true nếu là request gửi lại của eventId cũ.
     */
    @PostMapping("/entry")
    public ResponseEntity<?> entry(@Valid @RequestBody EntryRequest request) {
        // 1. Chuyển đổi dữ liệu từ DTO sang Command của tầng nghiệp vụ (Member 1)
        CheckInCommand command = new CheckInCommand(
                request.eventId(),
                request.gateId(),
                request.licensePlate(),
                request.vehicleType()
        );

        // 2. Gọi ParkingService xử lý nghiệp vụ check-in
        GateResult result = parkingService.checkIn(command);
        GateDecision decision = result.decision();

        // 3. Xử lý phản hồi theo kết quả quyết định của nghiệp vụ:
        if (decision.accepted()) {
            // Thành công: Trả về HTTP 201 Created kèm mã phiên và chỗ đỗ được cấp
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HEADER_EVENT_REPLAYED, String.valueOf(result.replayed()))
                    .body(EntryResponse.of(decision.sessionId(), decision.spotCode()));
        } else {
            // Bị từ chối (409 Conflict): Trả về mã lỗi nghiệp vụ (NO_AVAILABLE_SPOT, VEHICLE_ALREADY_PARKED, EVENT_ID_REUSED)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(HEADER_EVENT_REPLAYED, String.valueOf(result.replayed()))
                    .body(ApiErrorResponse.of(decision.code(), decision.message()));
        }
    }

    /**
     * API Xe ra khỏi bãi — Tiếp nhận sự kiện checkout tự động.
     * Endpoint: POST /api/v1/gates/exit
     *
     * @param request dữ liệu cổng ra (eventId, gateId, licensePlate)
     * @return HTTP 200 OK kèm ExitResponse nếu thành công;
     *         HTTP 409 Conflict kèm ApiErrorResponse nếu không tìm thấy phiên ACTIVE hoặc eventId bị tái sử dụng sai;
     *         Header X-Event-Replayed: true nếu là request gửi lại.
     */
    @PostMapping("/exit")
    public ResponseEntity<?> exit(@Valid @RequestBody ExitRequest request) {
        // 1. Chuyển đổi dữ liệu sang Command của tầng nghiệp vụ
        CheckOutCommand command = new CheckOutCommand(
                request.eventId(),
                request.gateId(),
                request.licensePlate()
        );

        // 2. Gọi ParkingService xử lý nghiệp vụ checkout
        GateResult result = parkingService.checkOut(command);
        GateDecision decision = result.decision();

        // 3. Xử lý phản hồi:
        if (decision.accepted()) {
            // Thành công: Trả về HTTP 200 OK kèm mã phiên và chỗ đỗ vừa được giải phóng
            return ResponseEntity.ok()
                    .header(HEADER_EVENT_REPLAYED, String.valueOf(result.replayed()))
                    .body(ExitResponse.of(decision.sessionId(), decision.spotCode()));
        } else {
            // Bị từ chối (409 Conflict): Trả về mã lỗi nghiệp vụ (NO_ACTIVE_SESSION, EVENT_ID_REUSED)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(HEADER_EVENT_REPLAYED, String.valueOf(result.replayed()))
                    .body(ApiErrorResponse.of(decision.code(), decision.message()));
        }
    }
}
