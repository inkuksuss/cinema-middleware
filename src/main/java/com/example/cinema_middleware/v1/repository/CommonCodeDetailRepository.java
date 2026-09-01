package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.key.CommonCodeDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonCodeDetailRepository extends JpaRepository<CommonCodeDetailRepository, CommonCodeDetailId> {
}
