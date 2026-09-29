package com.example.portfolio.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.cloud.storage.Storage;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class GcsServiceTest {

    @Mock
    private Storage storage;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletesExistingFileOnlyAfterDatabaseCommit() {
        GcsService service = new GcsService(storage, "portfolio-bucket");
        TransactionSynchronizationManager.initSynchronization();

        service.deleteAfterCommit("https://storage.googleapis.com/portfolio-bucket/1/old.webp");

        verify(storage, never()).delete("portfolio-bucket", "1/old.webp");
        synchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(storage).delete("portfolio-bucket", "1/old.webp");
    }

    @Test
    void deletesNewFileWhenDatabaseTransactionRollsBack() {
        GcsService service = new GcsService(storage, "portfolio-bucket");
        TransactionSynchronizationManager.initSynchronization();

        service.deleteOnRollback("https://storage.googleapis.com/portfolio-bucket/1/new.webp");
        synchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(storage).delete("portfolio-bucket", "1/new.webp");
    }

    private List<TransactionSynchronization> synchronizations() {
        return TransactionSynchronizationManager.getSynchronizations();
    }
}
