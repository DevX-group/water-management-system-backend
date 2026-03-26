package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.SentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentMessageRepository extends JpaRepository<SentMessage, Long> {
    List<SentMessage> findAllByOrderBySentDateDescSentTimeDescIdDesc();
}
