package com.example.cinema_middleware.v1.service.factory;

import com.example.cinema_middleware.v1.domain.entity.FileMaster;
import com.example.cinema_middleware.v1.service.dto.FileDto;
import org.springframework.util.Assert;

/**
 * 파일 응답 DTO 조립기.
 * 엔티티 → {@link FileDto} 변환의 유일한 지점이다.
 */
public final class FileDtoFactory {

    private FileDtoFactory() {
    }

    public static FileDto.Summary summary(FileMaster fileMaster) {
        Assert.notNull(fileMaster, "fileMaster can not be null");

        return new FileDto.Summary(
                fileMaster.getOriginalName(),
                fileMaster.getDestination(),
                fileMaster.getViewOrder()
        );
    }
}
