package com.backend.water_management_system.messaging.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.messaging.entity.ScheduledMessage;

import java.util.List;

@Repository
public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                    SELECT sm
                    FROM ScheduledMessage sm
                    WHERE sm.scheduleTime IS NOT NULL
                            AND (
                                    (sm.scheduleType = com.backend.water_management_system.messaging.enums.ScheduleType.ONE_TIME
                                        AND COALESCE(sm.oneTimeMessageSent, false) = false)
                                    OR sm.scheduleType = com.backend.water_management_system.messaging.enums.ScheduleType.RECURRING
                                )
            """)
    List<ScheduledMessage> findAllEmailSchedulableWithLock();
    // selects scheduled messages whose scheduleTime is not NULL and scheduleType is
    // (recurring or if onetime -> not sent)
    // it doesn't check scheduleDate is not NULL because in recurring messages it is
    // NULL
}
