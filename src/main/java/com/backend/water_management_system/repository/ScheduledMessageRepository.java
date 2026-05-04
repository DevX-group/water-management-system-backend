package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.ScheduledMessage;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT sm 
                FROM ScheduledMessage sm
                WHERE sm.scheduleTime IS NOT NULL   
                        AND (
                                (LOWER(TRIM(sm.scheduleType)) IN ('one-time', 'one time', 'onetime', 'one_time')
                                    AND COALESCE(sm.oneTimeMessageSent, false) = false)
                                OR LOWER(TRIM(sm.scheduleType)) = 'recurring'
                            )
        """)
    List<ScheduledMessage> findAllEmailSchedulableWithLock(); 
    //selects scheduled messages whose scheduleTime is not NULL and scheduleType is (recurring or if onetime -> not sent)
    //it doesn't check scheduleDate is not NULL because in recurring messages it is NULL
}
