package com.backend.water_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.entity.Message;

public interface MesageRepository extends JpaRepository<Message, Long> {

}
