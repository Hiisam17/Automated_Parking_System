package com.ktpm.parking.business.repository;

import java.util.function.Supplier;

public interface ParkingUnitOfWork {

    <T> T execute(Supplier<T> action);
}
