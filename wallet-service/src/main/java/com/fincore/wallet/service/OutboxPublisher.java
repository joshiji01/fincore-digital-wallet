package com.fincore.wallet.service;

import com.fincore.wallet.entity.OutboxEvent;
import com.fincore.wallet.repository.OutboxEventRepository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxPublisher {

    
    
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
        OutboxEventRepository repository,
        KafkaTemplate<String, String> kafkaTemplate) {

    this.repository = repository;
    this.kafkaTemplate = kafkaTemplate;
}

    @Scheduled(fixedDelay = 1000)
    public void publishEvents() {

        List<OutboxEvent> events =
                repository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {

            kafkaTemplate.send(
                    "transaction-events",
                    event.getAggregateId().toString(),
                    event.getPayload()
            ).whenComplete((result, exception) -> {

                if (exception == null) {
                    event.setProcessed(true);
                    repository.save(event);
                }
            });
        }
    }
}