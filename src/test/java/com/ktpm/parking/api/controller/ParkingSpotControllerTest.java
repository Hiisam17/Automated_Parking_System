package com.ktpm.parking.api.controller;

import com.ktpm.parking.api.dto.PageMeta;
import com.ktpm.parking.api.dto.ParkingSpotPageResponse;
import com.ktpm.parking.api.dto.ParkingSpotResponse;
import com.ktpm.parking.api.error.GlobalExceptionHandler;
import com.ktpm.parking.business.domain.ParkingSpotStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.exception.BusinessException;
import com.ktpm.parking.business.exception.ResourceNotFoundException;
import com.ktpm.parking.infrastructure.service.ParkingSpotAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tầng Web/Controller (MockMvc) cho ParkingSpotController.
 * Đảm bảo các API quản lý chỗ đỗ xe cho Admin hoạt động chuẩn OpenAPI.
 */
@ExtendWith(MockitoExtension.class)
class ParkingSpotControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ParkingSpotAdminService parkingSpotAdminService;

    @InjectMocks
    private ParkingSpotController parkingSpotController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(parkingSpotController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách chỗ đỗ: trả về HTTP 200 OK cùng dữ liệu phân trang")
    void shouldReturnSpotListWithPagination() throws Exception {
        var spot = new ParkingSpotResponse(1L, "A01", VehicleType.MOTORBIKE, ParkingSpotStatus.AVAILABLE, false);
        var pageMeta = PageMeta.of(0, 20, 1);
        var pageResponse = new ParkingSpotPageResponse(List.of(spot), pageMeta);

        when(parkingSpotAdminService.listParkingSpots(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/parking-spots")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").isEqualTo(1))
                .andExpect(jsonPath("$.items[0].code").isEqualTo("A01"))
                .andExpect(jsonPath("$.items[0].status").isEqualTo("AVAILABLE"))
                .andExpect(jsonPath("$.items[0].inMaintenance").isEqualTo(false))
                .andExpect(jsonPath("$.page.totalElements").isEqualTo(1));
    }

    @Test
    @DisplayName("Bật bảo trì thành công: trả về HTTP 201 Created kèm inMaintenance = true")
    void shouldStartMaintenanceSuccessfully() throws Exception {
        var updatedSpot = new ParkingSpotResponse(1L, "A01", VehicleType.MOTORBIKE, ParkingSpotStatus.AVAILABLE, true);
        when(parkingSpotAdminService.startMaintenance(1L)).thenReturn(updatedSpot);

        mockMvc.perform(post("/api/v1/parking-spots/1/maintenance"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isEqualTo(1))
                .andExpect(jsonPath("$.code").isEqualTo("A01"))
                .andExpect(jsonPath("$.inMaintenance").isEqualTo(true));
    }

    @Test
    @DisplayName("Bật bảo trì khi không tìm thấy chỗ đỗ: trả về HTTP 404 SPOT_NOT_FOUND")
    void shouldReturn404WhenSpotNotFoundOnStartMaintenance() throws Exception {
        when(parkingSpotAdminService.startMaintenance(999L))
                .thenThrow(new ResourceNotFoundException("SPOT_NOT_FOUND", "Không tìm thấy chỗ đỗ với ID: 999"));

        mockMvc.perform(post("/api/v1/parking-spots/999/maintenance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").isEqualTo("SPOT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Bật bảo trì cho chỗ đang có xe đỗ: trả về HTTP 409 SPOT_OCCUPIED")
    void shouldReturn409WhenSpotOccupiedOnStartMaintenance() throws Exception {
        when(parkingSpotAdminService.startMaintenance(2L))
                .thenThrow(new BusinessException("SPOT_OCCUPIED", "Chỗ đang có xe đỗ, không thể đưa vào bảo trì."));

        mockMvc.perform(post("/api/v1/parking-spots/2/maintenance"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").isEqualTo("SPOT_OCCUPIED"));
    }

    @Test
    @DisplayName("Bật bảo trì cho chỗ đã đang bảo trì: trả về HTTP 409 ALREADY_IN_MAINTENANCE")
    void shouldReturn409WhenSpotAlreadyInMaintenance() throws Exception {
        when(parkingSpotAdminService.startMaintenance(3L))
                .thenThrow(new BusinessException("ALREADY_IN_MAINTENANCE", "Chỗ đỗ này đã đang trong trạng thái bảo trì."));

        mockMvc.perform(post("/api/v1/parking-spots/3/maintenance"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").isEqualTo("ALREADY_IN_MAINTENANCE"));
    }

    @Test
    @DisplayName("Kết thúc bảo trì thành công: trả về HTTP 204 No Content")
    void shouldEndMaintenanceSuccessfully() throws Exception {
        doNothing().when(parkingSpotAdminService).endMaintenance(1L);

        mockMvc.perform(delete("/api/v1/parking-spots/1/maintenance"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Kết thúc bảo trì khi không tìm thấy chỗ: trả về HTTP 404 SPOT_NOT_FOUND")
    void shouldReturn404WhenSpotNotFoundOnEndMaintenance() throws Exception {
        doThrow(new ResourceNotFoundException("SPOT_NOT_FOUND", "Không tìm thấy chỗ đỗ"))
                .when(parkingSpotAdminService).endMaintenance(999L);

        mockMvc.perform(delete("/api/v1/parking-spots/999/maintenance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").isEqualTo("SPOT_NOT_FOUND"));
    }
}
