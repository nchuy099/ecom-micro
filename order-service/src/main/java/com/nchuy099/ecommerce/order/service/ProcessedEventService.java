package com.nchuy099.ecommerce.order.service;

import java.util.UUID;
import java.util.function.Supplier;

import com.nchuy099.ecommerce.order.entity.ProcessedEventEntity;
import com.nchuy099.ecommerce.order.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProcessedEventService {
    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    public boolean processOnce(UUID eventId, String eventType, Supplier<Boolean> handler) {
        String id = eventType + ":" + eventId;
        if (processedEventRepository.existsById(id)) {
            return false;
        }
        boolean handled = handler.get();
        if (!handled) {
            return false;
        }
        try {
            processedEventRepository.saveAndFlush(new ProcessedEventEntity(id, eventType));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
