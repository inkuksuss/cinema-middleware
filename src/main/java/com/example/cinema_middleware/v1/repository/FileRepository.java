package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.FileMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileMaster, Long> {
}
