package com.ktpm.parking.api.controller;

import com.ktpm.parking.api.dto.PageMeta;
import com.ktpm.parking.api.dto.ParkingSessionPageResponse;
import com.ktpm.parking.api.dto.ParkingSessionResponse;
import com.ktpm.parking.api.error.GlobalExceptionHandler;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.exception.ResourceNotFoundException;
import com.ktpm.parking.infrastructure.service.ParkingSessionAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tầng Web/Controller (MockMvc) cho ParkingSessionController.
 * Đảm bảo các API tra cứu danh sách phiên và xem chi tiết phiên cho Admin hoạt động chuẩn OpenAPI.
 */
@ExtendWith(MockitoExtension.class)
class ParkingSessionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ParkingSessionAdminService parkingSessionAdminService;

    @InjectMocks
    private ParkingSessionController parkingSessionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(parkingSessionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách phiên gửi xe: trả về HTTP 200 OK cùng dữ liệu phân trang")
    void shouldReturnSessionListWithPagination() throws Exception {
        var checkInAt = OffsetDateTime.of(2026, 10, 25, 8, 0, 0, 0, ZoneOffset.UTC);
        var session = new ParkingSessionResponse(
                101L,
                "29A-12345",
                VehicleType.MOTORBIKE,
                12L,
                "A05",
                checkInAt,
                null,
                ParkingSessionStatus.ACTIVE
        );
        var pageMeta = PageMeta.of(0, 20, 1);
        var pageResponse = new ParkingSessionPageResponse(List.of(session), pageMeta);

        when(parkingSessionAdminService.listParkingSessions(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/parking-sessions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(101))
                .andExpect(jsonPath("$.items[0].licensePlate").value("29A-12345"))
                .andExpect(jsonPath("$.items[0].spotCode").value("A05"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("Lọc danh sách phiên khi from > to: trả về HTTP 400 Bad Request kèm VALIDATION_ERROR")
    void shouldReturn400WhenFromIsAfterTo() throws Exception {
        when(parkingSessionAdminService.listParkingSessions(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Mốc thời gian 'from' phải nhỏ hơn hoặc bằng 'to'."));

        mockMvc.perform(get("/api/v1/parking-sessions")
                        .param("from", "2026-10-25T10:00:00Z")
                        .param("to", "2026-10-25T08:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Mốc thời gian 'from' phải nhỏ hơn hoặc bằng 'to'."));
    }

    @Test
    @DisplayName("Xem chi tiết một phiên gửi xe thành công: trả về HTTP 200 OK kèm thông tin phiên")
    void shouldReturnSessionDetailSuccessfully() throws Exception {
        var checkInAt = OffsetDateTime.of(2026, 10, 25, 8, 0, 0, 0, ZoneOffset.UTC);
        var checkOutAt = OffsetDateTime.of(2026, 10, 25, 10, 30, 0, 0, ZoneOffset.UTC);
        var session = new ParkingSessionResponse(
                101L,
                "29A-12345",
                VehicleType.MOTORBIKE,
                12L,
                "A05",
                checkInAt,
                checkOutAt,
                ParkingSessionStatus.COMPLETED
        );

        when(parkingSessionAdminService.getParkingSession(101L)).thenReturn(session);

        mockMvc.perform(get("/api/v1/parking-sessions/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.licensePlate").value("29A-12345"))
                .andExpect(jsonPath("$.spotId").value(12))
                .andExpect(jsonPath("$.spotCode").value("A05"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Xem chi tiết phiên khi không tìm thấy ID: trả về HTTP 404 SESSION_NOT_FOUND")
    void shouldReturn404WhenSessionNotFound() throws Exception {
        when(parkingSessionAdminService.getParkingSession(999L))
                .thenThrow(new ResourceNotFoundException("SESSION_NOT_FOUND", "Không tìm thấy phiên gửi xe với ID: 999"));

        mockMvc.perform(get("/api/v1/parking-sessions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Không tìm thấy phiên gửi xe với ID: 999"));
    }
}
