package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.ScheduledMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, Long> {
    @Query("""
            SELECT sm FROM ScheduledMessage sm
            WHERE sm.channels IS NOT NULL
            	AND LOWER(sm.channels) LIKE '%email%'
            	AND sm.scheduleTime IS NOT NULL
            	AND (
            		  (LOWER(sm.scheduleType) = 'one-time' AND COALESCE(sm.oneTimeEmailSent, false) = false)
            				OR LOWER(sm.scheduleType) = 'recurring'
            			)
            """)
    List<ScheduledMessage> findAllEmailSchedulable();
}
