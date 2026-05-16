package com.backend.water_management_system.messaging.repository;

import com.backend.water_management_system.messaging.entity.SentMessageFailure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SentMessageFailureRepository extends JpaRepository<SentMessageFailure, Long> {
    Page<SentMessageFailure> findBySentMessageId(Long sentMessageId, Pageable pageable);
}
