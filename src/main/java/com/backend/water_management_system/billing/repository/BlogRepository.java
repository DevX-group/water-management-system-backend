package com.backend.water_management_system.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.water_management_system.billing.entity.Blog;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
}