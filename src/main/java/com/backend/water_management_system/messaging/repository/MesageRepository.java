package com.backend.water_management_system.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.messaging.entity.Message;

public interface MesageRepository extends JpaRepository<Message, Long> {

}
