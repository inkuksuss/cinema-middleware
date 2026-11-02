package com.example.cinema_middleware.v1.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * application.yaml 의 타임아웃 설정이 실제 커넥션 풀·DB 세션·Redis 클라이언트에 반영되는지 확인한다.
 */
@SpringBootTest
class TimeoutConfigTest {

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired RedisConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("커넥션 풀 대기 3초, 드라이버 연결 1초·쿼리 응답 30초")
    void hikariTimeouts() {
        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertThat(hikari.getConnectionTimeout()).isEqualTo(3000L);
        assertThat(hikari.getDataSourceProperties())
                .containsEntry("connectTimeout", "1000")
                .containsEntry("socketTimeout", "30000");
    }

    @Test
    @DisplayName("앱 DB 세션의 행 잠금 대기는 5초")
    void innodbLockWaitTimeout() {
        Long lockWaitTimeout = jdbcTemplate.queryForObject("SELECT @@SESSION.innodb_lock_wait_timeout", Long.class);

        assertThat(lockWaitTimeout).isEqualTo(5L);
    }

    @Test
    @DisplayName("Redis 명령 응답 대기 1초")
    void redisCommandTimeout() {
        LettuceConnectionFactory lettuce = (LettuceConnectionFactory) redisConnectionFactory;

        assertThat(lettuce.getClientConfiguration().getCommandTimeout()).isEqualTo(Duration.ofSeconds(1));
    }
}
