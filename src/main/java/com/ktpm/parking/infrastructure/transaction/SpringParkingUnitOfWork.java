package com.ktpm.parking.infrastructure.transaction;

import com.ktpm.parking.business.repository.ParkingUnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

@Component
public class SpringParkingUnitOfWork implements ParkingUnitOfWork {

    private final TransactionTemplate transaction;

    public SpringParkingUnitOfWork(PlatformTransactionManager transactionManager) {
        transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        // Validate only this workflow's boundary, without changing the shared
        // transaction manager's behavior for other application services.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            Integer isolation = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
            if (isolation != null && isolation != TransactionDefinition.ISOLATION_READ_COMMITTED) {
                throw new IllegalTransactionStateException("Parking workflow requires READ_COMMITTED isolation");
            }
            if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                throw new IllegalTransactionStateException("Parking workflow requires a writable transaction");
            }
        }
        return transaction.execute(status -> action.get());
    }
}
