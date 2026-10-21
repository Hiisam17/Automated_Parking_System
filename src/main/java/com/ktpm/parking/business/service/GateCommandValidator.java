package com.ktpm.parking.business.service;

import com.ktpm.parking.business.exception.BusinessException;

final class GateCommandValidator {

    private static final int EVENT_ID_MAX_LENGTH = 64;
    private static final int GATE_ID_MAX_LENGTH = 64;
    private static final int LICENSE_PLATE_MIN_LENGTH = 4;
    private static final int LICENSE_PLATE_MAX_LENGTH = 20;

    private GateCommandValidator() {
    }

    static void validate(CheckInCommand command) {
        if (command == null) {
            reject("Yêu cầu check-in không được để trống.");
        }
        validateCommon(command.eventId(), command.gateId(), command.licensePlate());
        if (command.vehicleType() == null) {
            reject("Loại xe không được để trống.");
        }
    }

    static void validate(CheckOutCommand command) {
        if (command == null) {
            reject("Yêu cầu checkout không được để trống.");
        }
        validateCommon(command.eventId(), command.gateId(), command.licensePlate());
    }

    private static void validateCommon(String eventId, String gateId, String licensePlate) {
        validateText(eventId, "eventId", 1, EVENT_ID_MAX_LENGTH);
        validateText(gateId, "gateId", 1, GATE_ID_MAX_LENGTH);
        validateText(
                licensePlate,
                "licensePlate",
                LICENSE_PLATE_MIN_LENGTH,
                LICENSE_PLATE_MAX_LENGTH
        );
    }

    private static void validateText(String value, String field, int minLength, int maxLength) {
        if (value == null || value.isBlank()) {
            reject(field + " không được để trống.");
        }
        int length = value.strip().length();
        if (length < minLength || length > maxLength) {
            reject(field + " phải có độ dài từ " + minLength + " đến " + maxLength + " ký tự.");
        }
    }

    private static void reject(String message) {
        throw new BusinessException(ParkingError.VALIDATION_ERROR.code(), message);
    }
}
