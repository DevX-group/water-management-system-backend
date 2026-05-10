package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.TriggeredMessage;
import com.backend.water_management_system.enums.TriggerType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriggeredMessageRepository extends JpaRepository<TriggeredMessage, Long> {
    List<TriggeredMessage> findByTriggerTypeAndActiveTrue(TriggerType triggerType);
}
