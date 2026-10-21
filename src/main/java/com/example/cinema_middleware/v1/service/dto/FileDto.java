package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.FileMaster;
import org.springframework.util.Assert;

/**
 * 파일 도메인 서비스 응답 DTO 모음.
 * 상위 계층(controller)을 참조하지 않는다.
 */
public final class FileDto {

    private FileDto() {
    }

    /** 첨부파일 요약 */
    public record Summary(
            String originalName,
            String destination,
            Integer viewOrder
    ) {
        public static Summary from(FileMaster fileMaster) {
            Assert.notNull(fileMaster, "fileMaster can not be null");

            return new Summary(
                    fileMaster.getOriginalName(),
                    fileMaster.getDestination(),
                    fileMaster.getViewOrder()
            );
        }
    }
}
