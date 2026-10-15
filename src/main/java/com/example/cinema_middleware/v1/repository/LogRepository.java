package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.LogMaster;
import org.springframework.data.repository.CrudRepository;

public interface LogRepository extends CrudRepository<LogMaster, Long> {
}
