package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
