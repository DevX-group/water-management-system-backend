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
            WHERE sm.scheduleTime IS NOT NULL
                    	AND (
             (LOWER(TRIM(sm.scheduleType)) IN ('one-time', 'one time', 'onetime', 'one_time')
                 AND COALESCE(sm.oneTimeEmailSent, false) = false)
            OR LOWER(TRIM(sm.scheduleType)) = 'recurring'
                    			)
                    """)
    List<ScheduledMessage> findAllEmailSchedulable();
}
