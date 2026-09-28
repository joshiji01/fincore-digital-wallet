package com.fincore.user.service;

import com.fincore.user.entity.OutboxEvent;
import com.fincore.user.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {

            kafkaTemplate
                    .send(
                            "user-events",
                            event.getAggregateId().toString(),
                            event.getPayload()
                    )
                    .whenComplete((result, exception) -> {

                        if (exception == null) {
                            event.setProcessed(true);
                            outboxEventRepository.save(event);
                        }
                    });
        }
    }
}