package com.example.cinema_middleware.v1.service.dto;

/**
 * 파일 도메인 서비스 응답 DTO 모음.
 * 상위 계층(controller)을 참조하지 않는다. 엔티티 변환은 {@code service.factory.FileDtoFactory} 가 담당한다.
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
    }
}
