package com.example.cinema_middleware;

import com.example.cinema_middleware.v1.domain.entity.CommonCodeDetail;
import com.example.cinema_middleware.v1.domain.entity.CommonCodeGroup;
import com.example.cinema_middleware.v1.repository.CommonCodeDetailRepository;
import com.example.cinema_middleware.v1.repository.CommonCodeGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.Transactional;

@ConfigurationPropertiesScan
@EnableJpaAuditing
@SpringBootApplication
public class CinemaMiddlewareApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinemaMiddlewareApplication.class, args);
	}


	@Autowired
	CommonCodeDetailRepository commonCodeDetailRepository;
	@Autowired
	CommonCodeGroupRepository commonCodeGroupRepository;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void initCommonCode() {
		// TODO
	}

}

