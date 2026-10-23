package com.ktpm.parking.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktpm.parking.api.dto.EntryRequest;
import com.ktpm.parking.api.dto.ExitRequest;
import com.ktpm.parking.api.error.GlobalExceptionHandler;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.service.CheckInCommand;
import com.ktpm.parking.business.service.CheckOutCommand;
import com.ktpm.parking.business.service.GateDecision;
import com.ktpm.parking.business.service.GateResult;
import com.ktpm.parking.business.service.ParkingError;
import com.ktpm.parking.business.service.ParkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tầng Web/Controller (MockMvc) cho GateController.
 * Kiểm tra các trường hợp xe vào (/entry) và xe ra (/exit) bao gồm:
 * thành công, thất bại nghiệp vụ (409), gửi lại sự kiện (header replay) và lỗi dữ liệu (400).
 */
@ExtendWith(MockitoExtension.class)
class GateControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ParkingService parkingService;

    @InjectMocks
    private GateController gateController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gateController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==========================================
    // CÁC TEST CASES CHO API CỔNG VÀO (ENTRY)
    // ==========================================

    @Test
    @DisplayName("Xe vào thành công: trả về HTTP 201 Created kèm mã phiên và chỗ đỗ được phân bổ")
    void shouldAcceptEntryAndReturnCreated() throws Exception {
        var decision = GateDecision.accepted(101L, "A05");
        when(parkingService.checkIn(any(CheckInCommand.class))).thenReturn(GateResult.newResult(decision));

        var request = new EntryRequest("evt-001", "IN-01", "29A-12345", VehicleType.MOTORBIKE);

        mockMvc.perform(post("/api/v1/gates/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(GateController.HEADER_EVENT_REPLAYED, "false"))
                .andExpect(jsonPath("$.status").isEqualTo("ACCEPTED"))
                .andExpect(jsonPath("$.sessionId").isEqualTo(101))
                .andExpect(jsonPath("$.assignedSpot").isEqualTo("A05"));
    }

    @Test
    @DisplayName("Xe vào khi sự kiện bị gửi lại (Replay): trả về HTTP 201 kèm header X-Event-Replayed: true")
    void shouldReturnReplayedHeaderOnEntryReplay() throws Exception {
        var decision = GateDecision.accepted(101L, "A05");
        when(parkingService.checkIn(any(CheckInCommand.class))).thenReturn(GateResult.replay(decision));

        var request = new EntryRequest("evt-001", "IN-01", "29A-12345", VehicleType.MOTORBIKE);

        mockMvc.perform(post("/api/v1/gates/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(GateController.HEADER_EVENT_REPLAYED, "true"))
                .andExpect(jsonPath("$.status").isEqualTo("ACCEPTED"))
                .andExpect(jsonPath("$.sessionId").isEqualTo(101))
                .andExpect(jsonPath("$.assignedSpot").isEqualTo("A05"));
    }

    @Test
    @DisplayName("Xe vào thất bại do hết chỗ: trả về HTTP 409 Conflict với mã lỗi NO_AVAILABLE_SPOT")
    void shouldReturnConflictWhenParkingIsFull() throws Exception {
        var decision = GateDecision.rejected(ParkingError.NO_AVAILABLE_SPOT);
        when(parkingService.checkIn(any(CheckInCommand.class))).thenReturn(GateResult.newResult(decision));

        var request = new EntryRequest("evt-002", "IN-01", "29A-99999", VehicleType.CAR);

        mockMvc.perform(post("/api/v1/gates/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(header().string(GateController.HEADER_EVENT_REPLAYED, "false"))
                .andExpect(jsonPath("$.error.code").isEqualTo("NO_AVAILABLE_SPOT"));
    }

    @Test
    @DisplayName("Xe vào thất bại do xe đã ở trong bãi: trả về HTTP 409 Conflict với mã VEHICLE_ALREADY_PARKED")
    void shouldReturnConflictWhenVehicleAlreadyParked() throws Exception {
        var decision = GateDecision.rejected(ParkingError.VEHICLE_ALREADY_PARKED);
        when(parkingService.checkIn(any(CheckInCommand.class))).thenReturn(GateResult.newResult(decision));

        var request = new EntryRequest("evt-003", "IN-01", "29A-12345", VehicleType.MOTORBIKE);

        mockMvc.perform(post("/api/v1/gates/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").isEqualTo("VEHICLE_ALREADY_PARKED"));
    }

    @Test
    @DisplayName("Xe vào gửi thiếu thông tin: trả về HTTP 400 Bad Request kèm mã VALIDATION_ERROR")
    void shouldReturnBadRequestWhenEntryPayloadIsInvalid() throws Exception {
        // Gửi eventId rỗng và biển số quá ngắn (< 4 ký tự)
        var request = new EntryRequest("", "IN-01", "12", VehicleType.CAR);

        mockMvc.perform(post("/api/v1/gates/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR"));
    }

    // ==========================================
    // CÁC TEST CASES CHO API CỔNG RA (EXIT)
    // ==========================================

    @Test
    @DisplayName("Xe ra thành công: trả về HTTP 200 OK kèm mã phiên và chỗ đỗ được giải phóng")
    void shouldAcceptExitAndReturnOk() throws Exception {
        var decision = GateDecision.accepted(101L, "A05");
        when(parkingService.checkOut(any(CheckOutCommand.class))).thenReturn(GateResult.newResult(decision));

        var request = new ExitRequest("evt-004", "OUT-01", "29A-12345");

        mockMvc.perform(post("/api/v1/gates/exit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(GateController.HEADER_EVENT_REPLAYED, "false"))
                .andExpect(jsonPath("$.status").isEqualTo("ACCEPTED"))
                .andExpect(jsonPath("$.sessionId").isEqualTo(101))
                .andExpect(jsonPath("$.releasedSpot").isEqualTo("A05"));
    }

    @Test
    @DisplayName("Xe ra thất bại do không tìm thấy xe trong bãi: trả về HTTP 409 Conflict với NO_ACTIVE_SESSION")
    void shouldReturnConflictWhenActiveSessionNotFound() throws Exception {
        var decision = GateDecision.rejected(ParkingError.NO_ACTIVE_SESSION);
        when(parkingService.checkOut(any(CheckOutCommand.class))).thenReturn(GateResult.newResult(decision));

        var request = new ExitRequest("evt-005", "OUT-01", "29A-88888");

        mockMvc.perform(post("/api/v1/gates/exit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(header().string(GateController.HEADER_EVENT_REPLAYED, "false"))
                .andExpect(jsonPath("$.error.code").isEqualTo("NO_ACTIVE_SESSION"));
    }

    @Test
    @DisplayName("Xe ra gửi thiếu thông tin: trả về HTTP 400 Bad Request kèm mã VALIDATION_ERROR")
    void shouldReturnBadRequestWhenExitPayloadIsInvalid() throws Exception {
        var request = new ExitRequest("", "OUT-01", "");

        mockMvc.perform(post("/api/v1/gates/exit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR"));
    }
}
