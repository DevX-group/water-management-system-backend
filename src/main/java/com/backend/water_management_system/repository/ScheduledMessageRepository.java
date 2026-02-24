package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.ScheduledMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, Long> {
}
