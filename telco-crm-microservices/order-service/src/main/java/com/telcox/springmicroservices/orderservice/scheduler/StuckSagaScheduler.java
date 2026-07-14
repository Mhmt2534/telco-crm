package com.telcox.springmicroservices.orderservice.scheduler;

import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;
import com.telcox.springmicroservices.orderservice.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StuckSagaScheduler {

    private final SagaStateRepository sagaStateRepository;

    @Value("${app.saga.stuck-threshold-minutes:5}")
    private int stuckThresholdMinutes;

    @Scheduled(cron = "${app.saga.stuck-job-cron:0 */5 * * * *}")
    public void findStuckSagas() {
        log.info("Starting scheduled job to find stuck sagas. Threshold: {} minutes", stuckThresholdMinutes);
        
        OffsetDateTime thresholdTime = OffsetDateTime.now().minusMinutes(stuckThresholdMinutes);
        List<SagaStatus> targetStatuses = List.of(SagaStatus.STARTED, SagaStatus.IN_PROGRESS);
        
        List<SagaState> stuckSagas = sagaStateRepository.findByLastUpdatedBeforeAndStatusIn(thresholdTime, targetStatuses);
        
        if (stuckSagas.isEmpty()) {
            log.info("No stuck sagas found.");
            return;
        }

        log.warn("Found {} stuck sagas. Manual intervention may be required.", stuckSagas.size());
        
        for (SagaState saga : stuckSagas) {
            log.warn("STUCK SAGA DETECTED -> SagaID: {}, OrderID: {}, CurrentStep: {}, Status: {}, LastUpdated: {}",
                    saga.getSagaId(), saga.getOrderId(), saga.getCurrentStep(), saga.getStatus(), saga.getLastUpdated());
        }
        
        log.info("Finished scheduled job to find stuck sagas.");
    }
}
