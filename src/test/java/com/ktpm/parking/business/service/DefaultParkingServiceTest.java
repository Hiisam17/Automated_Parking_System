package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.VehicleType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultParkingServiceTest {

    @Test
    void shouldDelegateEachWorkflowToItsUseCaseService() {
        CheckInService checkInService = mock(CheckInService.class);
        CheckOutService checkOutService = mock(CheckOutService.class);
        ParkingService service = new DefaultParkingService(checkInService, checkOutService);
        CheckInCommand checkIn = new CheckInCommand(
                "evt-entry", "IN-01", "29A-12345", VehicleType.MOTORBIKE
        );
        CheckOutCommand checkOut = new CheckOutCommand("evt-exit", "OUT-01", "29A-12345");
        GateResult entryResult = GateResult.newResult(GateDecision.accepted(101L, "A05"));
        GateResult exitResult = GateResult.newResult(GateDecision.accepted(101L, "A05"));
        when(checkInService.checkIn(checkIn)).thenReturn(entryResult);
        when(checkOutService.checkOut(checkOut)).thenReturn(exitResult);

        assertThat(service.checkIn(checkIn)).isSameAs(entryResult);
        assertThat(service.checkOut(checkOut)).isSameAs(exitResult);
        verify(checkInService).checkIn(checkIn);
        verify(checkOutService).checkOut(checkOut);
    }
}
