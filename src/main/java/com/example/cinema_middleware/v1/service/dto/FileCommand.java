package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.FileUsage;
import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 도메인 서비스 입력 커맨드.
 */
public final class FileCommand {

    private FileCommand() {
    }

    public record Create(
            Long targetId,
            TableType targetType,
            FileUsage usageType,
            Integer viewOrder,
            MultipartFile file
    ) {
    }
}
