package com.backend.water_management_system.messaging.repository;

import com.backend.water_management_system.messaging.entity.TriggeredMessage;
import com.backend.water_management_system.messaging.enums.TriggerType;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriggeredMessageRepository extends JpaRepository<TriggeredMessage, Long> {
    List<TriggeredMessage> findByTriggerTypeAndActiveTrue(TriggerType triggerType);
}
